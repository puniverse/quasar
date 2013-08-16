/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorUtil;
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.LocalActorUtil;
import static co.paralleluniverse.actors.LocalActorUtil.isLocal;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.ShutdownMessage;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.reply;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.replyError;
import co.paralleluniverse.actors.behaviors.Supervisor.AddChildMessage;
import co.paralleluniverse.actors.behaviors.Supervisor.ChildSpec;
import co.paralleluniverse.actors.behaviors.Supervisor.GetChildMessage;
import co.paralleluniverse.actors.behaviors.Supervisor.RemoveChildMessage;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jsr166e.ConcurrentHashMapV8;
import jsr166e.ForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * An actor that supervises, and if necessary, restarts other actors.
 *
 *
 * <p/>
 * If an actor needs to know the identity of its siblings, it should add them to the supervisor manually. For that, it needs to know the identity
 * of its supervisor. To do that, pass {@link LocalActor#self self()} to that actor's constructor in the {@link #Supervisor(String, RestartStrategy, SuspendableCallable) initializer}
 * or the {@link #init() init} method. Alternatively, simply call {@link LocalActor#self self()} in the actor's constructor.
 *
 * This works because the children are constructed from specs (provided they have not been constructed by the caller) during the supervisor's run,
 * so calling {@link LocalActor#self self()} anywhere in the construction process would return the supervisor.
 *
 * @author pron
 */
public class SupervisorActor extends GenBehaviorActor {
    private static final Logger LOG = LoggerFactory.getLogger(SupervisorActor.class);
    private final RestartStrategy restartStrategy;
    private List<ChildSpec> childSpec;
    private final List<ChildEntry> children = new ArrayList<ChildEntry>();
    private final ConcurrentMap<Object, ChildEntry> childrenById = new ConcurrentHashMapV8<Object, ChildEntry>();

    public SupervisorActor(Strand strand, String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, Initializer initializer) {
        super(name, initializer, strand, mailboxConfig);
        this.restartStrategy = restartStrategy;
        this.childSpec = null;
    }

    public SupervisorActor(Strand strand, String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        super(name, null, strand, mailboxConfig);
        this.restartStrategy = restartStrategy;
        this.childSpec = childSpec;
    }

    public SupervisorActor(Strand strand, String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(strand, name, mailboxConfig, restartStrategy, Arrays.asList(childSpec));
    }

    //<editor-fold defaultstate="collapsed" desc="Behavior boilerplate">
    /////////// Behavior boilerplate ///////////////////////////////////
    @Override
    protected Supervisor makeRef(ActorRef<Object> ref) {
        return new Supervisor.Local(ref);
    }

    @Override
    public Supervisor ref() {
        return (Supervisor) super.ref();
    }

    @Override
    protected Supervisor self() {
        return ref();
    }

    @Override
    public Supervisor spawn(ForkJoinPool fjPool) {
        return (Supervisor) super.spawn(fjPool);
    }

    @Override
    public Supervisor spawn() {
        return (Supervisor) super.spawn();
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public SupervisorActor(Strand strand, String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy) {
        this(strand, name, mailboxConfig, restartStrategy, (Initializer) null);
    }

    public SupervisorActor(String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy) {
        this(null, name, mailboxConfig, restartStrategy, (Initializer) null);
    }

    public SupervisorActor(String name, RestartStrategy restartStrategy) {
        this(null, name, null, restartStrategy, (Initializer) null);
    }

    public SupervisorActor(String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, Initializer initializer) {
        this(null, name, mailboxConfig, restartStrategy, initializer);
    }

    public SupervisorActor(String name, RestartStrategy restartStrategy, Initializer initializer) {
        this(null, name, null, restartStrategy, initializer);
    }

    public SupervisorActor(RestartStrategy restartStrategy) {
        this(null, null, null, restartStrategy, (Initializer) null);
    }

    ///
    public SupervisorActor(String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        this(null, name, mailboxConfig, restartStrategy, childSpec);
    }

    public SupervisorActor(String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(null, name, mailboxConfig, restartStrategy, childSpec);
    }

    public SupervisorActor(String name, RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        this(null, name, null, restartStrategy, childSpec);
    }

    public SupervisorActor(String name, RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(null, name, null, restartStrategy, childSpec);
    }

    public SupervisorActor(RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        this(null, null, null, restartStrategy, childSpec);
    }

    public SupervisorActor(RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(null, null, null, restartStrategy, childSpec);
    }
    //</editor-fold>

    public static SupervisorActor currentSupervisor() {
        return (SupervisorActor)Actor.<Object, Void>currentActor();
    }
    
    @Override
    public Logger log() {
        return LOG;
    }

    @Override
    protected void init() throws SuspendExecution {
        if (getInitializer() != null)
            getInitializer().init();
        else {
            if (childSpec != null) {
                try {
                    if (getInitializer() != null)
                        throw new IllegalStateException("Cannot provide a supervisor with both a child-spec list as well as an initializer");
                    if (!SupervisorActor.class.equals(this.getClass()))
                        throw new IllegalStateException("Cannot provide a subclassed supervisor with a child-spec list");

                    for (ChildSpec cs : childSpec)
                        addChild(cs);
                    this.childSpec = null;
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
        }
    }

    @Override
    protected void onStart() throws InterruptedException, SuspendExecution {
        if (LOG.isInfoEnabled()) {
            //org.apache.logging.log4j.ThreadContext.push(this.toString());
            MDC.put("self", this.toString());
        }
        super.onStart();
    }

    @Override
    protected final void handleMessage(Object m1) throws InterruptedException, SuspendExecution {
        if (m1 instanceof GenRequestMessage) {
            final GenRequestMessage req = (GenRequestMessage) m1;
            try {
                if (req instanceof GetChildMessage) {
                    reply(req, getChild(((GetChildMessage) req).name));
                } else if (req instanceof AddChildMessage) {
                    reply(req, addChild(((AddChildMessage) req).spec));
                } else if (req instanceof RemoveChildMessage) {
                    final RemoveChildMessage m = (RemoveChildMessage) req;
                    reply(req, removeChild(m.name, m.terminate));
                }
            } catch (Exception e) {
                replyError(req, e);
            }
        }
    }

    @Override
    protected void onTerminate(Throwable cause) throws SuspendExecution, InterruptedException {
        super.onTerminate(cause);

        shutdownChildren();
        childrenById.clear();
        children.clear();

        if (LOG.isInfoEnabled()) {
            //org.apache.logging.log4j.ThreadContext.pop();
            MDC.remove("self");
        }
    }

    private ChildEntry addChild1(ChildSpec spec) {
        LOG.debug("Adding child {}", spec);
        ActorRef actor = null;
        if (spec.builder instanceof ActorRef) {
            actor = (ActorRef) spec.builder;
            if (findEntry(actor) != null)
                throw new SupervisorException("Supervisor " + this + " already supervises actor " + actor);
        }

        Object id = spec.getId();
        if (id == null && actor != null)
            id = actor.getName();
        if (id != null && findEntryById(id) != null)
            throw new SupervisorException("Supervisor " + this + " already supervises an actor by the name " + id);
        final ChildEntry child = new ChildEntry(spec, actor);
        children.add(child);
        if (id != null)
            childrenById.put(id, child);
        return child;
    }

    protected final <T extends ActorRef<M>, M> T addChild(ChildSpec spec) throws SuspendExecution, InterruptedException {
        verifyInActor();
        final ChildEntry child = addChild1(spec);

        ActorRef<?> actor = spec.builder instanceof ActorRef ? (ActorRef<?>) spec.builder : null;
        if (actor == null)
            actor = start(child);
        else
            start(child, actor);

        return (T) actor;
    }

    protected <T extends ActorRef<M>, M> T getChild(Object name) {
        verifyInActor();
        final ChildEntry child = findEntryById(name);
        if (child == null)
            return null;
        return (T) child.actor;
    }

    protected final boolean removeChild(Object id, boolean terminate) throws SuspendExecution, InterruptedException {
        verifyInActor();
        final ChildEntry child = findEntryById(id);
        if (child == null) {
            LOG.warn("Child {} not found", id);
            return false;
        }

        LOG.debug("Removing child {}", child);
        if (child.actor != null) {
            unwatch(child);

            if (terminate)
                shutdownChild(child, false);
        }

        removeChild(child, null);

        return true;
    }

    private void removeChild(ChildEntry child, Iterator<ChildEntry> iter) {
        if (child.spec.getId() != null)
            childrenById.remove(child.spec.getId());
        if (iter != null)
            iter.remove();
        else
            children.remove(child);
    }

    @Override
    protected final void handleLifecycleMessage(LifecycleMessage m) {
        boolean handled = false;
        try {
            if (m instanceof ExitMessage) {
                final ExitMessage death = (ExitMessage) m;
                if (death.getWatch() != null) {
                    final ActorRef actor = death.actor;
                    final ChildEntry child = findEntry(actor);

                    if (child != null) {
                        LOG.info("Detected child death: " + child + ". cause: ", death.cause);
                        if (!restartStrategy.onChildDeath(this, child, death.cause)) {
                            LOG.info("Supervisor {} giving up.", this);
                            shutdown();
                        }
                        handled = true;
                    }
                }
            }
        } catch (InterruptedException e) {
            getStrand().interrupt();
        }
        if (!handled)
            super.handleLifecycleMessage(m);
    }

    private boolean tryRestart(ChildEntry child, Throwable cause, long now, Iterator<ChildEntry> it, boolean isDead) throws InterruptedException {
        verifyInActor();
        switch (child.spec.mode) {
            case TRANSIENT:
                if (isDead && cause == null) {
                    removeChild(child, it);
                    return true;
                }
            // fall through
            case PERMANENT:
                LOG.info("Supervisor trying to restart child {}. (cause: {})", child, cause);
                final ActorRef actor = child.actor;
                shutdownChild(child, true);
                child.restartHistory.addRestart(now);
                final int numRestarts = child.restartHistory.numRestarts(now - child.spec.unit.toMillis(child.spec.duration));
                if (LOG.isDebugEnabled())
                    LOG.debug("Child {} has been restarted {} times in the last {} {}s", child, numRestarts, child.spec.duration, child.spec.unit);
                if (numRestarts > child.spec.maxRestarts) {
                    LOG.info(this + ": too many restarts for child {}. Giving up.", actor);
                    return false;
                }
                start(child);
                return true;
            case TEMPORARY:
                if (!isDead)
                    shutdownChild(child, false);
                removeChild(child, it);
                return true;
            default:
                throw new AssertionError();
        }
    }

    private ActorRef<?> start(ChildEntry child) {
        final ActorRef old = child.actor;
        if (old != null && !LocalActorUtil.isDone(old))
            throw new IllegalStateException("Actor " + child.actor + " cannot be restarted because it is not dead");

        final Actor actor = child.spec.builder.build();
        if (actor.getName() == null && child.spec.id != null)
            actor.setName(child.spec.id);

        LOG.info("{} starting child {}", this, actor);

        if (old != null && actor.getMonitor() == null && isLocal(old) && LocalActorUtil.getMonitor(old) != null)
            actor.setMonitor(LocalActorUtil.getMonitor(old));
        if (actor.getMonitor() != null)
            actor.getMonitor().addRestart();

        final Strand strand;
        if (actor.getStrand() != null)
            strand = actor.getStrand();
        else
            strand = createStrandForActor(child.actor != null && isLocal(child.actor) ? LocalActorUtil.getStrand(child.actor) : null, actor);

        try {
            strand.start();
        } catch (IllegalThreadStateException e) {
            LOG.info("Child {} has already been started.", actor);
        }

        return start(child, actor.ref());
    }

    private ActorRef start(ChildEntry child, ActorRef actor) {
        child.actor = actor;
        child.watch = watch(actor);

        return actor;
    }

    private void shutdownChild(ChildEntry child, boolean beforeRestart) throws InterruptedException {
        if (child.actor != null) {
            unwatch(child);
            if (!isLocal(child.actor) || !LocalActorUtil.isDone(child.actor)) {
                LOG.info("{} shutting down child {}", this, child.actor);
                ActorUtil.sendOrInterrupt(child.actor, new ShutdownMessage(this.ref()));
            }

            if (isLocal(child.actor)) {
                try {
                    joinChild(child);
                } finally {
                    if (!beforeRestart)
                        LocalActorUtil.stopMonitor(child.actor);
                }
            }
            if (!beforeRestart)
                child.actor = null;
        }
    }

    private void shutdownChildren() throws InterruptedException {
        LOG.info("{} shutting down all children.", this);
        for (ChildEntry child : children) {
            if (child.actor != null) {
                unwatch(child);
                ActorUtil.sendOrInterrupt(child.actor, new ShutdownMessage(this.ref()));
            }
        }

        for (ChildEntry child : children) {
            if (child.actor != null && isLocal(child.actor)) {
                try {
                    joinChild(child);
                } finally {
                    LocalActorUtil.stopMonitor(child.actor); // must be done after join to avoid a race with the actor
                }
            }
            child.actor = null;
        }
    }

    private boolean joinChild(ChildEntry child) throws InterruptedException {
        final ActorRef actor = child.actor;

        LOG.debug("Joining child {}", child);
        if (child.actor != null) {
            try {
                LocalActorUtil.join(actor, child.spec.shutdownDeadline, TimeUnit.MILLISECONDS);
                LOG.debug("Child {} terminated normally", child.actor);
                return true;
            } catch (ExecutionException ex) {
                LOG.info("Child {} terminated with exception {}", child.actor, ex.getCause());
                return true;
            } catch (TimeoutException ex) {
                LOG.warn("Child {} shutdown timeout. Interrupting...", child.actor);
                // is this the best we can do?
                LocalActorUtil.getStrand(actor).interrupt();

                try {
                    LocalActorUtil.join(actor, child.spec.shutdownDeadline, TimeUnit.MILLISECONDS);
                    return true;
                } catch (ExecutionException e) {
                    LOG.info("Child {} terminated with exception {}", child.actor, ex.getCause());
                    return true;
                } catch (TimeoutException e) {
                    LOG.warn("Child {} could not shut down...", child.actor);

                    LocalActorUtil.stopMonitor(child.actor);
                    LocalActorUtil.unregister(child.actor);
                    child.actor = null;

                    return false;
                }
            }
        } else
            return true;
    }

    private void unwatch(ChildEntry child) {
        if (child.actor != null && child.watch != null) {
            unwatch(child.actor, child.watch);
            child.watch = null;
        }
    }

    private Strand createStrandForActor(Strand oldStrand, Actor actor) {
        final Strand strand;
        if (oldStrand != null)
            strand = Strand.clone(oldStrand, actor);
        else
            strand = new Fiber(actor);
        actor.setStrand(strand);
        return strand;
    }

    private ChildEntry findEntry(ActorRef actor) {
        if (actor.getName() != null) {
            ChildEntry child = findEntryById(actor.getName());
            if (child != null)
                return child;
        }
        for (ChildEntry child : children) {
            if (Objects.equals(child.actor, actor))
                return child;
        }
        return null;
    }

    private ChildEntry findEntryById(Object name) {
        return childrenById.get(name);
    }

    private long now() {
        return System.nanoTime() / 1000000;
    }

    public enum RestartStrategy {
        ESCALATE {
            @Override
            boolean onChildDeath(SupervisorActor supervisor, ChildEntry child, Throwable cause) throws InterruptedException {
                return false;
            }
        },
        ONE_FOR_ONE {
            @Override
            boolean onChildDeath(SupervisorActor supervisor, ChildEntry child, Throwable cause) throws InterruptedException {
                return supervisor.tryRestart(child, cause, supervisor.now(), null, true);
            }
        },
        ALL_FOR_ONE {
            @Override
            boolean onChildDeath(SupervisorActor supervisor, ChildEntry child, Throwable cause) throws InterruptedException {
                if (child.spec.mode == Supervisor.ChildMode.TEMPORARY
                        || (child.spec.mode == Supervisor.ChildMode.TRANSIENT && cause == null)) {
                    if (!supervisor.tryRestart(child, cause, supervisor.now(), null, true))
                        return false;
                } else {
                    supervisor.shutdownChildren();
                    for (Iterator<ChildEntry> it = supervisor.children.iterator(); it.hasNext();) {
                        final ChildEntry c = it.next();
                        if (!supervisor.tryRestart(c, c == child ? cause : null, supervisor.now(), it, c == child))
                            return false;
                    }
                }
                return true;
            }
        },
        REST_FOR_ONE {
            @Override
            boolean onChildDeath(SupervisorActor supervisor, ChildEntry child, Throwable cause) throws InterruptedException {
                if (child.spec.mode == Supervisor.ChildMode.TEMPORARY
                        || (child.spec.mode == Supervisor.ChildMode.TRANSIENT && cause == null)) {
                    if (!supervisor.tryRestart(child, cause, supervisor.now(), null, true))
                        return false;
                } else {
                    boolean found = false;
                    for (Iterator<ChildEntry> it = supervisor.children.iterator(); it.hasNext();) {
                        final ChildEntry c = it.next();
                        if (c == child)
                            found = true;

                        if (found && !supervisor.tryRestart(c, c == child ? cause : null, supervisor.now(), it, c == child))
                            return false;
                    }
                }
                return true;
            }
        };

        abstract boolean onChildDeath(SupervisorActor supervisor, ChildEntry child, Throwable cause) throws InterruptedException;
    }

    private static class ChildEntry {
        final ChildSpec spec;
        final RestartHistory restartHistory;
        Object watch;
        volatile ActorRef<?> actor;

        public ChildEntry(ChildSpec info) {
            this(info, null);
        }

        public ChildEntry(ChildSpec info, ActorRef<?> actor) {
            this.spec = info;
            this.restartHistory = new RestartHistory(info.maxRestarts + 1);

            this.actor = actor;
        }

        @Override
        public String toString() {
            return "ActorEntry{" + "info=" + spec + " actor=" + actor + '}';
        }
    }

    private static class RestartHistory {
        private final long[] restarts;
        private int index;

        public RestartHistory(int windowSize) {
            this.restarts = new long[windowSize];
            this.index = 0;
        }

        public void addRestart(long now) {
            restarts[index] = now;
            index = mod(index + 1);
        }

        public int numRestarts(long since) {
            int count = 0;
            for (int i = mod(index - 1); i != index; i = mod(i - 1)) {
                if (restarts[i] < since) // || restarts[i] == 0L is implied
                    break;
                count++;
            }
            if (restarts[index] >= since) // || restarts[i] == 0L is implied
                count++;
            return count;
        }

        private int mod(int i) {
            // could be made fast by forcing restarts.length to be a power of two, but for now, we don't need this to be fast.
            if (i >= restarts.length)
                return i - restarts.length;
            if (i < 0)
                return i + restarts.length;
            return i;
        }
    }
}
