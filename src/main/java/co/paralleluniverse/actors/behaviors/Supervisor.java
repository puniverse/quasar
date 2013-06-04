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
import co.paralleluniverse.actors.ActorBuilder;
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jsr166e.ConcurrentHashMapV8;
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
public class Supervisor extends LocalActor<Object, Void> implements GenBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(Supervisor.class);
    private final RestartStrategy restartStrategy;
    private final SuspendableCallable<List<ChildSpec>> initializer;
    private List<ChildSpec> childSpec;
    private final List<ChildEntry> children = new ArrayList<ChildEntry>();
    private final ConcurrentMap<Object, ChildEntry> childrenById = new ConcurrentHashMapV8<Object, ChildEntry>();

    public Supervisor(Strand strand, String name, int mailboxSize, RestartStrategy restartStrategy, SuspendableCallable<List<ChildSpec>> initializer) {
        super(strand, name, mailboxSize);
        this.restartStrategy = restartStrategy;

        this.initializer = initializer;
        this.childSpec = null;
    }

    public Supervisor(Strand strand, String name, int mailboxSize, RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        super(strand, name, mailboxSize);
        this.restartStrategy = restartStrategy;

        this.initializer = null;
        this.childSpec = childSpec;
    }

    public Supervisor(Strand strand, String name, int mailboxSize, RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(strand, name, mailboxSize, restartStrategy, Arrays.asList(childSpec));
    }

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public Supervisor(Strand strand, String name, int mailboxSize, RestartStrategy restartStrategy) {
        this(strand, name, mailboxSize, restartStrategy, (SuspendableCallable) null);
    }

    public Supervisor(String name, int mailboxSize, RestartStrategy restartStrategy) {
        this(null, name, mailboxSize, restartStrategy, (SuspendableCallable) null);
    }

    public Supervisor(String name, RestartStrategy restartStrategy) {
        this(null, name, -1, restartStrategy, (SuspendableCallable) null);
    }

    public Supervisor(String name, int mailboxSize, RestartStrategy restartStrategy, SuspendableCallable<List<ChildSpec>> initializer) {
        this(null, name, mailboxSize, restartStrategy, initializer);
    }

    public Supervisor(String name, RestartStrategy restartStrategy, SuspendableCallable<List<ChildSpec>> initializer) {
        this(null, name, -1, restartStrategy, initializer);
    }

    public Supervisor(RestartStrategy restartStrategy) {
        this(null, null, -1, restartStrategy, (SuspendableCallable) null);
    }

    ///
    public Supervisor(String name, int mailboxSize, RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        this(null, name, mailboxSize, restartStrategy, childSpec);
    }

    public Supervisor(String name, int mailboxSize, RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(null, name, mailboxSize, restartStrategy, childSpec);
    }

    public Supervisor(String name, RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        this(null, name, -1, restartStrategy, childSpec);
    }

    public Supervisor(String name, RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(null, name, -1, restartStrategy, childSpec);
    }

    public Supervisor(RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        this(null, null, -1, restartStrategy, childSpec);
    }

    public Supervisor(RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(null, null, -1, restartStrategy, childSpec);
    }
    //</editor-fold>

    public <Message, V> LocalActor<Message, V> getChild(Object name) {
        final ChildEntry child = findEntryById(name);
        if (child == null)
            return null;
        return (LocalActor<Message, V>) child.actor;
    }

    @Override
    public void shutdown() {
        send(new ShutdownMessage(LocalActor.self()));
    }

    private List<ChildSpec> init0() throws SuspendExecution, InterruptedException {
        if (childSpec != null) {
            if (initializer != null)
                throw new IllegalStateException("Cannot provide a supervisor with both a child-spec list as well as an initializer");
            if (!Supervisor.class.equals(this.getClass()))
                throw new IllegalStateException("Cannot provide a subclassed supervisor with a child-spec list");
            final List<ChildSpec> cs = this.childSpec;
            this.childSpec = null;
            return cs;
        }
        return init();
    }

    protected List<ChildSpec> init() throws SuspendExecution, InterruptedException {
        if (initializer != null)
            return initializer.run();
        return null;
    }

    protected void createChildren() throws SuspendExecution, InterruptedException {
        final List<ChildSpec> _childSpec = init0();
        if (_childSpec != null) {
            for (ChildSpec cs : _childSpec)
                addChild1(cs);
        }
    }

    @Override
    protected final Void doRun() throws InterruptedException, SuspendExecution {
        if (LOG.isInfoEnabled()) {
            //org.apache.logging.log4j.ThreadContext.push(this.toString());
            MDC.put("self", this.toString());
        }

        try {
            createChildren();

            // start children
            for (ChildEntry child : children)
                start(child);

            for (;;) {
                final Object m1 = receive(); // we care only about lifecycle messages
                if (m1 instanceof ShutdownMessage)
                    break;
                else if (m1 instanceof GenRequestMessage) {
                    final GenRequestMessage req = (GenRequestMessage) m1;
                    try {
                        if (m1 instanceof AddChildMessage) {
                            final AddChildMessage m = (AddChildMessage) m1;
                            RequestReplyHelper.reply(m, addChild(m.info));
                        }
                    } catch (Exception e) {
                        req.getFrom().send(new GenErrorResponseMessage(req.getId(), e));
                    }
                }
            }
        } catch (InterruptedException e) {
        } catch (Exception e) {
            LOG.info("Exception!", e);
            throw Exceptions.rethrow(e);
        } finally {
            LOG.info("Supervisor {} shutting down.", this);
            shutdownChildren();
            childrenById.clear();
            children.clear();

            if (LOG.isInfoEnabled()) {
                //org.apache.logging.log4j.ThreadContext.pop();
                MDC.remove("self");
            }
        }

        return null;
    }

    private ChildEntry addChild1(ChildSpec spec) {
        LOG.debug("Adding child {}", spec);
        LocalActor actor = null;
        if (spec.builder instanceof LocalActor) {
            actor = (LocalActor) spec.builder;
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

    public final Actor addChild(ChildSpec spec) throws SuspendExecution, InterruptedException {
        if (isInActor()) {
            final ChildEntry child = addChild1(spec);
            final LocalActor actor = spec.builder instanceof LocalActor ? (LocalActor) spec.builder : null;
            if (actor == null)
                start(child);
            else
                start(child, actor);

            return actor;
        } else {
            final GenResponseMessage res = RequestReplyHelper.call(this, new AddChildMessage(RequestReplyHelper.from(), randtag(), spec));
            return ((GenValueResponseMessage<Actor>) res).getValue();
        }
    }

    public final boolean removeChild(Object id, boolean terminate) throws SuspendExecution, InterruptedException {
        if (isInActor()) {
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
                else
                    unwatch(child);
            }

            removeChild(child, null);

            return true;
        } else {
            final GenResponseMessage res = RequestReplyHelper.call(this, new RemoveChildMessage(RequestReplyHelper.from(), randtag(), id, terminate));
            return ((GenValueResponseMessage<Boolean>) res).getValue();
        }
    }

    private void removeChild(ChildEntry child, Iterator<ChildEntry> iter) {
        if (child.info.getId() != null)
            childrenById.remove(child.info.getId());
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
                if (death.getWatch() != null && death.actor instanceof LocalActor) {
                    final LocalActor actor = (LocalActor) death.actor;
                    final ChildEntry child = findEntry(actor);

                    if (child != null) {
                        LOG.info("Detected child death: " + child + ". cause: ", death.cause);
                        if (!restartStrategy.onChildDeath(this, child, death.cause)) {
                            LOG.info("Supervisor {} giving up.", this);
                            getStrand().interrupt();
                        }
                        handled = true;
                    }
                }
            } else if (m instanceof ShutdownMessage) {
                handled = true;
                getStrand().interrupt();
            }
            if (!handled)
                super.handleLifecycleMessage(m);
        } catch (InterruptedException e) {
            getStrand().interrupt();
        }
    }

    private boolean tryRestart(ChildEntry child, Throwable cause, long now, Iterator<ChildEntry> it) throws InterruptedException {
        LOG.info("Supervisor trying to restart child {}. (cause: {})", child, cause);
        verifyInActor();
        switch (child.info.mode) {
            case TRANSIENT:
                if (cause == null)
                    return true;
            // fall through
            case PERMANENT:
                final Actor actor = child.actor;
                shutdownChild(child, true);
                child.restartHistory.addRestart(now);
                final int numRestarts = child.restartHistory.numRestarts(now - child.info.unit.toMillis(child.info.duration));
                if (LOG.isDebugEnabled())
                    LOG.debug("Child {} has been restarted {} times in the last {} {}s", child, numRestarts, child.info.duration, child.info.unit);
                if (numRestarts > child.info.maxRestarts) {
                    LOG.info(this + ": too many restarts for child {}. Giving up.", actor);
                    return false;
                }
                start(child);
                return true;
            case TEMPORARY:
                shutdownChild(child, false);
                removeChild(child, it);
                return true;
            default:
                throw new AssertionError();
        }
    }

    private LocalActor start(ChildEntry child) {
        final LocalActor old = child.actor;
        if (old != null && !old.isDone())
            throw new IllegalStateException("Actor " + child.actor + " cannot be restarted because it is not dead");

        final LocalActor actor = child.info.builder.build();
        if (actor.getName() == null && child.info.id != null)
            actor.setName(child.info.id);

        LOG.info("{} starting child {}", this, actor);

        if (old != null && actor.getMonitor() == null && old.getMonitor() != null)
            actor.setMonitor(old.getMonitor());
        if (actor.getMonitor() != null)
            actor.getMonitor().addRestart();

        return start(child, actor);
    }

    private LocalActor start(ChildEntry child, LocalActor actor) {
        final Strand strand;
        if (actor.getStrand() != null)
            strand = actor.getStrand();
        else
            strand = createStrandForActor(child.actor != null ? child.actor.getStrand() : null, actor);

        child.actor = actor;
        child.watch = watch(actor);

        try {
            strand.start();
        } catch (IllegalThreadStateException e) {
            // strand has already been started
        }
        return actor;
    }

    private void shutdownChild(ChildEntry child, boolean beforeRestart) throws InterruptedException {
        if (child.actor != null) {
            unwatch(child);
            if (!child.actor.isDone()) {
                LOG.info("{} shutting down child {}", this, child.actor);
                ((Actor) child.actor).send(new ShutdownMessage(this));
            }
            try {
                joinChild(child);
            } finally {
                if (!beforeRestart) {
                    child.actor.stopMonitor();
                    child.actor = null;
                }
            }
        }
    }

    private void shutdownChildren() throws InterruptedException {
        LOG.info("{} shutting down all children.", this);
        for (ChildEntry child : children) {
            if (child.actor != null) {
                unwatch(child);
                ((Actor) child.actor).send(new ShutdownMessage(this));
            }
        }

        for (ChildEntry child : children) {
            if (child.actor != null) {
                try {
                    joinChild(child);
                    child.actor.stopMonitor(); // must be done after join to avoid a race with the actor
                } finally {
                    child.actor = null;
                }
            }
        }
    }

    private boolean joinChild(ChildEntry child) throws InterruptedException {
        if (child.actor != null) {
            try {
                child.actor.join(child.info.shutdownDeadline, TimeUnit.MILLISECONDS);
                return true;
            } catch (ExecutionException ex) {
                LOG.info("Child {} died with exception {}", child.actor, ex.getCause());
                return true;
            } catch (TimeoutException ex) {
                LOG.warn("Child {} shutdown timeout. Interrupting...", child.actor);
                // is this the best we can do?
                child.actor.getStrand().interrupt();

                try {
                    child.actor.join(child.info.shutdownDeadline, TimeUnit.MILLISECONDS);
                    return true;
                } catch (ExecutionException e) {
                    LOG.info("Child {} died with exception {}", child.actor, ex.getCause());
                    return true;
                } catch (TimeoutException e) {
                    LOG.warn("Child {} could not shut down...", child.actor);

                    child.actor.stopMonitor();
                    child.actor.unregister();
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

    private Strand createStrandForActor(Strand oldStrand, LocalActor actor) {
        final Strand strand;
        if (oldStrand != null)
            strand = Strand.clone(oldStrand, actor);
        else
            strand = new Fiber(actor);
        actor.setStrand(strand);
        return strand;
    }

    private ChildEntry findEntry(LocalActor actor) {
        if (actor.getName() != null) {
            ChildEntry child = findEntryById(actor.getName());
            if (child != null)
                return child;
        }
        for (ChildEntry child : children) {
            if (child.actor == actor)
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

    public enum ChildMode {
        PERMANENT, TRANSIENT, TEMPORARY
    };

    public enum RestartStrategy {
        ONE_FOR_ONE {
            @Override
            boolean onChildDeath(Supervisor supervisor, ChildEntry child, Throwable cause) throws InterruptedException {
                return supervisor.tryRestart(child, cause, supervisor.now(), null);
            }
        },
        ALL_FOR_ONE {
            @Override
            boolean onChildDeath(Supervisor supervisor, ChildEntry child, Throwable cause) throws InterruptedException {
                supervisor.shutdownChildren();
                for (Iterator<ChildEntry> it = supervisor.children.iterator(); it.hasNext();) {
                    final ChildEntry c = it.next();
                    if (!supervisor.tryRestart(c, cause, supervisor.now(), it))
                        return false;
                }
                return true;
            }
        },
        REST_FOR_ONE {
            @Override
            boolean onChildDeath(Supervisor supervisor, ChildEntry child, Throwable cause) throws InterruptedException {
                boolean found = false;
                for (Iterator<ChildEntry> it = supervisor.children.iterator(); it.hasNext();) {
                    final ChildEntry c = it.next();
                    if (c == child)
                        found = true;

                    if (found && !supervisor.tryRestart(c, cause, supervisor.now(), it))
                        return false;
                }
                return true;
            }
        };

        abstract boolean onChildDeath(Supervisor supervisor, ChildEntry child, Throwable cause) throws InterruptedException;
    }

    private static class AddChildMessage extends GenRequestMessage {
        final ChildSpec info;

        public AddChildMessage(Actor from, Object id, ChildSpec info) {
            super(from, id);
            this.info = info;
        }
    }

    private static class RemoveChildMessage extends GenRequestMessage {
        final Object name;
        final boolean terminate;

        public RemoveChildMessage(Actor from, Object id, Object name, boolean terminate) {
            super(from, id);
            this.name = name;
            this.terminate = terminate;
        }
    }

    public static class ChildSpec {
        final Object id;
        final ActorBuilder<?, ?> builder;
        final ChildMode mode;
        final int maxRestarts;
        final long duration;
        final TimeUnit unit;
        final long shutdownDeadline;

        public ChildSpec(Object id, ChildMode mode, int maxRestarts, long duration, TimeUnit unit, long shutdownDeadline, ActorBuilder<?, ?> builder) {
            this.id = id;
            this.builder = builder;
            this.mode = mode;
            this.maxRestarts = maxRestarts;
            this.duration = duration;
            this.unit = unit;
            this.shutdownDeadline = shutdownDeadline;
        }

        public Object getId() {
            return id;
        }

        public ActorBuilder<?, ?> getBuilder() {
            return builder;
        }

        public ChildMode getMode() {
            return mode;
        }

        public int getMaxRestarts() {
            return maxRestarts;
        }

        public long getDuration() {
            return duration;
        }

        public TimeUnit getDurationUnit() {
            return unit;
        }

        public long getShutdownDeadline() {
            return shutdownDeadline;
        }

        @Override
        public String toString() {
            return "ActorInfo{" + "builder: " + builder + ", mode: " + mode + ", maxRestarts: " + maxRestarts + ", duration: " + duration + ", unit: " + unit + ", shutdownDeadline: " + shutdownDeadline + '}';
        }
    }

    private static class ChildEntry {
        final ChildSpec info;
        final RestartHistory restartHistory;
        Object watch;
        volatile LocalActor<?, ?> actor;

        public ChildEntry(ChildSpec info) {
            this(info, null);
        }

        public ChildEntry(ChildSpec info, LocalActor<?, ?> actor) {
            this.info = info;
            this.restartHistory = new RestartHistory(info.maxRestarts + 1);

            this.actor = actor;
        }

        @Override
        public String toString() {
            return "ActorEntry{" + "info=" + info + " actor=" + actor + '}';
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
