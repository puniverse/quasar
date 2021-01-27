/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.actors.LocalActor;
import static co.paralleluniverse.actors.LocalActor.isLocal;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.ShutdownMessage;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.reply;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.replyError;
import co.paralleluniverse.actors.behaviors.Supervisor.ChildSpec;
import co.paralleluniverse.actors.behaviors.Supervisor.AddChildMessage;
import co.paralleluniverse.actors.behaviors.Supervisor.GetChildMessage;
import co.paralleluniverse.actors.behaviors.Supervisor.GetChildrenMessage;
import co.paralleluniverse.actors.behaviors.Supervisor.RemoveChildMessage;
import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactory;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * An actor that supervises, and if necessary, restarts other actors.
 *
 * <p>
 * If an actor needs to know the identity of its siblings, it should add them to the supervisor manually. For that, it needs to know the identity
 * of its supervisor. To do that, pass {@link Actor#self self()} to that actor's constructor in the {@link Initializer initializer}
 * or the {@link #init() init} method. Alternatively, simply call {@link Actor#self self()}, which will return the supervisor actor, in the actor's constructor.</p>
 *
 * This works because the children are constructed from specs (provided they have not been constructed by the caller) during the supervisor's run,
 * so calling {@link Actor#self self()} anywhere in the construction process would return the supervisor.
 *
 * @author pron
 */
public class SupervisorActor extends BehaviorActor {
    private static final Logger LOG = LoggerFactory.getLogger(SupervisorActor.class);
    private final RestartStrategy restartStrategy;
    private List<ChildSpec> childSpec;
    private final List<ChildEntry> children = new ArrayList<>();
    private final ConcurrentMap<Object, ChildEntry> childrenById = MapUtil.newConcurrentHashMap();

    /**
     * Constructs a new supervisor with no children. Children may be added later via {@link Supervisor#addChild(Supervisor.ChildSpec) Supervisor.addChild}.
     *
     * @param strand          this actor's strand.
     * @param name            the actor name (may be {@code null}).
     * @param mailboxConfig   this actor's mailbox settings.
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param initializer     an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     */
    public SupervisorActor(Strand strand, String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, Initializer initializer) {
        super(name, initializer, strand, mailboxConfig);
        this.restartStrategy = restartStrategy;
        this.childSpec = null;
    }

    /**
     * Constructs a new supervisor with a given list of children.
     *
     * @param strand          this actor's strand.
     * @param name            the actor name (may be {@code null}).
     * @param mailboxConfig   this actor's mailbox settings.
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param childSpec       the supervisor's children
     */
    public SupervisorActor(Strand strand, String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        super(name, null, strand, mailboxConfig);
        this.restartStrategy = restartStrategy;
        this.childSpec = childSpec;
    }

    /**
     * Constructs a new supervisor with a given list of children.
     *
     * @param strand          this actor's strand.
     * @param name            the actor name (may be {@code null}).
     * @param mailboxConfig   this actor's mailbox settings.
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param childSpec       the supervisor's children
     */
    public SupervisorActor(Strand strand, String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(strand, name, mailboxConfig, restartStrategy, Arrays.asList(childSpec));
    }

    //<editor-fold defaultstate="collapsed" desc="Behavior boilerplate">
    /////////// Behavior boilerplate ///////////////////////////////////
    @Override
    protected Supervisor makeRef(ActorRef<Object> ref) {
        return new Supervisor(ref);
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
    public Supervisor spawn(StrandFactory sf) {
        return (Supervisor) super.spawn(sf);
    }

    @Override
    public Supervisor spawn(FiberFactory ff) {
        return (Supervisor) super.spawn(ff);
    }

    @Override
    public Supervisor spawn() {
        return (Supervisor) super.spawn();
    }

    @Override
    public Supervisor spawnThread() {
        return (Supervisor) super.spawnThread();
    }

    public static SupervisorActor currentSupervisor() {
        return (SupervisorActor) Actor.<Object, Void>currentActor();
    }

    @Override
    public Logger log() {
        return LOG;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    /**
     * Constructs a new supervisor with no children. Children may be added later via {@link Supervisor#addChild(Supervisor.ChildSpec) Supervisor.addChild}.
     *
     * @param strand          this actor's strand.
     * @param name            the actor name (may be {@code null}).
     * @param mailboxConfig   this actor's mailbox settings.
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     */
    public SupervisorActor(Strand strand, String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy) {
        this(strand, name, mailboxConfig, restartStrategy, (Initializer) null);
    }

    /**
     * Constructs a new supervisor with no children. Children may be added later via {@link Supervisor#addChild(Supervisor.ChildSpec) Supervisor.addChild}.
     *
     * @param name            the actor name (may be {@code null}).
     * @param mailboxConfig   this actor's mailbox settings.
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     */
    public SupervisorActor(String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy) {
        this(null, name, mailboxConfig, restartStrategy, (Initializer) null);
    }

    /**
     * Constructs a new supervisor with no children. Children may be added later via {@link Supervisor#addChild(Supervisor.ChildSpec) Supervisor.addChild}.
     *
     * @param name            the actor name (may be {@code null}).
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     */
    public SupervisorActor(String name, RestartStrategy restartStrategy) {
        this(null, name, null, restartStrategy, (Initializer) null);
    }

    /**
     * Constructs a new supervisor with no children. Children may be added later via {@link Supervisor#addChild(Supervisor.ChildSpec) Supervisor.addChild}.
     *
     * @param name            the actor name (may be {@code null}).
     * @param mailboxConfig   this actor's mailbox settings.
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param initializer     an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     */
    public SupervisorActor(String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, Initializer initializer) {
        this(null, name, mailboxConfig, restartStrategy, initializer);
    }

    /**
     * Constructs a new supervisor with no children. Children may be added later via {@link Supervisor#addChild(Supervisor.ChildSpec) Supervisor.addChild}.
     *
     * @param name            the actor name (may be {@code null}).
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param initializer     an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     */
    public SupervisorActor(String name, RestartStrategy restartStrategy, Initializer initializer) {
        this(null, name, null, restartStrategy, initializer);
    }

    /**
     * Constructs a new supervisor with no children. Children may be added later via {@link Supervisor#addChild(Supervisor.ChildSpec) Supervisor.addChild}.
     *
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     */
    public SupervisorActor(RestartStrategy restartStrategy) {
        this(null, null, null, restartStrategy, (Initializer) null);
    }

    ///
    /**
     * Constructs a new supervisor with a given list of children.
     *
     * @param name            the actor name (may be {@code null}).
     * @param mailboxConfig   this actor's mailbox settings.
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param childSpec       the supervisor's children
     */
    public SupervisorActor(String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        this(null, name, mailboxConfig, restartStrategy, childSpec);
    }

    /**
     * Constructs a new supervisor with a given list of children.
     *
     * @param name            the actor name (may be {@code null}).
     * @param mailboxConfig   this actor's mailbox settings.
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param childSpec       the supervisor's children
     */
    public SupervisorActor(String name, MailboxConfig mailboxConfig, RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(null, name, mailboxConfig, restartStrategy, childSpec);
    }

    /**
     * Constructs a new supervisor with a given list of children.
     *
     * @param name            the actor name (may be {@code null}).
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param childSpec       the supervisor's children
     */
    public SupervisorActor(String name, RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        this(null, name, null, restartStrategy, childSpec);
    }

    /**
     * Constructs a new supervisor with a given list of children.
     *
     * @param name            the actor name (may be {@code null}).
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param childSpec       the supervisor's children
     */
    public SupervisorActor(String name, RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(null, name, null, restartStrategy, childSpec);
    }

    /**
     * Constructs a new supervisor with a given list of children.
     *
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param childSpec       the supervisor's children
     */
    public SupervisorActor(RestartStrategy restartStrategy, List<ChildSpec> childSpec) {
        this(null, null, null, restartStrategy, childSpec);
    }

    /**
     * Constructs a new supervisor with a given list of children.
     *
     * @param restartStrategy the supervisor's {@link RestartStrategy restart strategy}
     * @param childSpec       the supervisor's children
     */
    public SupervisorActor(RestartStrategy restartStrategy, ChildSpec... childSpec) {
        this(null, null, null, restartStrategy, childSpec);
    }
    //</editor-fold>

    @Override
    protected void init() throws InterruptedException, SuspendExecution {
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
        if (log().isInfoEnabled()) {
            //org.apache.logging.log4j.ThreadContext.push(this.toString());
            MDC.put("self", this.toString());
        }
        super.onStart();
    }

    @Override
    protected final void handleMessage(Object m1) throws InterruptedException, SuspendExecution {
        if (m1 instanceof RequestMessage) {
            final RequestMessage req = (RequestMessage) m1;
            try {
                if (req instanceof GetChildMessage) {
                    reply(req, getChild(((GetChildMessage) req).name));
                } else if (req instanceof GetChildrenMessage) {
                    reply(req, getChildren());
                } else if (req instanceof AddChildMessage) {
                    reply(req, addChild(((AddChildMessage) req).spec));
                } else if (req instanceof RemoveChildMessage) {
                    final RemoveChildMessage m = (RemoveChildMessage) req;
                    reply(req, m.name instanceof ActorRef ? removeChild((ActorRef<?>) m.name, m.terminate) : removeChild(m.name, m.terminate));
                }
            } catch (Exception e) {
                replyError(req, e);
            }
        } else if (m1 instanceof ExitMessage) {
            final ExitMessage death = (ExitMessage) m1;
            final ActorRef actor = death.actor;
            final ChildEntry child = findEntry(actor);

            if (child != null) {
                log().info("Detected child death: " + child + ". cause: ", death.cause);
                if (!restartStrategy.onChildDeath(this, child, death.cause)) {
                    log().info("Supervisor {} giving up.", this);
                    shutdown();
                }
            }
        }
    }

    @Override
    protected void onTerminate(Throwable cause) throws SuspendExecution, InterruptedException {
        super.onTerminate(cause);

        shutdownChildren();
        childrenById.clear();
        children.clear();

        if (log().isInfoEnabled()) {
            //org.apache.logging.log4j.ThreadContext.pop();
            MDC.remove("self");
        }
    }

    private ChildEntry addChild1(ChildSpec spec) {
        log().debug("Adding child {}", spec);
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

    /**
     * Adds a new child actor to the supervisor. If the child has not been started, it will be started by the supervisor.
     *
     * @param spec the {@link ChildSpec child's spec}.
     * @return the actor (possibly after it has been started by the supervisor).
     * @throws InterruptedException
     */
    protected final <T extends ActorRef<M>, M> T addChild(ChildSpec spec) throws SuspendExecution, InterruptedException {
        verifyInActor();
        final ChildEntry child = addChild1(spec);

        ActorRef<?> actor = null;
        if (spec.builder instanceof Actor) {
            final Actor a = ((Actor) spec.builder);
            actor = a.isStarted() ? a.ref() : a.spawn();
        }
        if (actor == null)
            actor = start(child);
        else
            start(child, actor);

        return (T) actor;
    }

    /**
     * Retrieves a child actor by its {@link ChildSpec#getId() id}
     *
     * @param id the child's {@link ChildSpec#getId() id} in the supervisor.
     * @return the child, if found; {@code null} if the child was not found
     */
    protected <T extends ActorRef<M>, M> T getChild(Object id) {
        verifyInActor();
        final ChildEntry child = findEntryById(id);
        if (child == null)
            return null;
        return (T) child.actor;
    }

    /**
     * Retrieves the children actor references as an immutable list.
     *
     * @return the children {@link ActorRef}s.
     */
    protected List<? extends ActorRef<?>> getChildren() {
        verifyInActor();
        final ImmutableList.Builder builder = new ImmutableList.Builder();
        for (final ChildEntry ce : children)
            builder.add(ce.actor);
        return builder.build();
    }

    /**
     * Removes a child actor from the supervisor.
     *
     * @param id        the child's {@link ChildSpec#getId() id} in the supervisor.
     * @param terminate whether or not the supervisor should terminate the actor
     * @return {@code true} if the actor has been successfully removed from the supervisor; {@code false} if the child was not found.
     * @throws InterruptedException
     */
    protected final boolean removeChild(Object id, boolean terminate) throws SuspendExecution, InterruptedException {
        verifyInActor();
        final ChildEntry child = findEntryById(id);
        if (child == null) {
            log().warn("Child {} not found", id);
            return false;
        }

        log().debug("Removing child {}", child);
        if (child.actor != null) {
            unwatch(child);

            if (terminate)
                shutdownChild(child, false);
        }

        removeChild(child, null);
        return true;
    }

    /**
     * Removes a child actor from the supervisor.
     *
     * @param actor     the actor
     * @param terminate whether or not the supervisor should terminate the actor
     * @return {@code true} if the actor has been successfully removed from the supervisor; {@code false} if the child was not found.
     * @throws InterruptedException
     */
    protected final boolean removeChild(ActorRef actor, boolean terminate) throws SuspendExecution, InterruptedException {
        verifyInActor();
        final ChildEntry child = findEntry(actor);
        if (child == null) {
            log().warn("Child {} not found", actor);
            return false;
        }

        log().debug("Removing child {}", child);
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
    protected final Object handleLifecycleMessage(LifecycleMessage m) {
        if (m instanceof ExitMessage) {
            final ExitMessage death = (ExitMessage) m;
            if (death.getWatch() != null)
                return death;
        }
        super.handleLifecycleMessage(m);
        return null;
    }

    private boolean tryRestart(ChildEntry child, Throwable cause, long now, Iterator<ChildEntry> it, boolean isDead) throws SuspendExecution, InterruptedException {
        verifyInActor();
        switch (child.spec.mode) {
            case TRANSIENT:
                if (isDead && cause == null) {
                    removeChild(child, it);
                    return true;
                }
            // fall through
            case PERMANENT:
                log().info("Supervisor trying to restart child {}. (cause: {})", child, cause);
                final ActorRef actor = child.actor;
                shutdownChild(child, true);
                child.restartHistory.addRestart(now);
                final int numRestarts = child.restartHistory.numRestarts(now - child.spec.unit.toMillis(child.spec.duration));
                if (log().isDebugEnabled())
                    log().debug("Child {} has been restarted {} times in the last {} {}s", child, numRestarts, child.spec.duration, child.spec.unit);
                if (numRestarts > child.spec.maxRestarts) {
                    log().info(this + ": too many restarts for child {}. Giving up.", actor);
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

    private ActorRef<?> start(ChildEntry child) throws SuspendExecution {
        final ActorRef old = child.actor;
        if (old != null && !LocalActor.isDone(old))
            throw new IllegalStateException("Actor " + child.actor + " cannot be restarted because it is not dead");

        final Actor actor = child.spec.builder.build();
        if (actor.getName() == null && child.spec.id != null)
            actor.setName(child.spec.id);

        log().info("{} starting child {}", this, actor);

        if (old != null && actor.getMonitor() == null && isLocal(old) && LocalActor.getMonitor(old) != null)
            actor.setMonitor(LocalActor.getMonitor(old));
        if (actor.getMonitor() != null)
            actor.getMonitor().addRestart();

        final Strand strand;
        if (actor.getStrand() != null)
            strand = actor.getStrand();
        else
            strand = createStrandForActor(child.actor != null && isLocal(child.actor) ? LocalActor.getStrand(child.actor) : null, actor);

        try {
            strand.start();
        } catch (IllegalThreadStateException e) {
            log().info("Child {} has already been started.", actor);
        }

        return start(child, actor.ref());
    }

    private ActorRef start(ChildEntry child, ActorRef actor) {
        child.actor = actor;
        child.watch = watch(actor);

        return actor;
    }

    private void shutdownChild(ChildEntry child, boolean beforeRestart) throws SuspendExecution, InterruptedException {
        if (child.actor != null) {
            unwatch(child);
            if (!isLocal(child.actor) || !LocalActor.isDone(child.actor)) {
                log().info("{} shutting down child {}", this, child.actor);
                ActorUtil.sendOrInterrupt(child.actor, new ShutdownMessage(this.ref()));
            }

            if (isLocal(child.actor)) {
                try {
                    joinChild(child);
                } finally {
                    if (!beforeRestart && child.actor != null)
                        LocalActor.stopMonitor(child.actor);
                }
            }
            if (!beforeRestart)
                child.actor = null;
        }
    }

    private void shutdownChildren() throws SuspendExecution, InterruptedException {
        log().info("{} shutting down all children.", this);
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
                    if (child.actor != null)
                        LocalActor.stopMonitor(child.actor); // must be done after join to avoid a race with the actor
                }
            }
            child.actor = null;
        }
    }

    private boolean joinChild(ChildEntry child) throws SuspendExecution, InterruptedException {
        final ActorRef actor = child.actor;

        log().debug("Joining child {}", child);
        if (child.actor != null) {
            try {
                LocalActor.join(actor, child.spec.shutdownDeadline, TimeUnit.MILLISECONDS);
                log().debug("Child {} terminated normally", child.actor);
                return true;
            } catch (ExecutionException ex) {
                log().info("Child {} terminated with exception {}", child.actor, ex.getCause());
                return true;
            } catch (TimeoutException ex) {
                log().warn("Child {} shutdown timeout. Interrupting...", child.actor);
                // is this the best we can do?
                LocalActor.getStrand(actor).interrupt();

                try {
                    LocalActor.join(actor, child.spec.shutdownDeadline, TimeUnit.MILLISECONDS);
                    return true;
                } catch (ExecutionException e) {
                    log().info("Child {} terminated with exception {}", child.actor, ex.getCause());
                    return true;
                } catch (TimeoutException e) {
                    log().warn("Child {} could not shut down...", child.actor);

                    LocalActor.stopMonitor(child.actor);
                    LocalActor.unregister(child.actor);
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

    /**
     * Specifies a supervisor's strategy in the event a child dies. Not every child death triggers the strategy. It is only triggered
     * when a {@link Supervisor.ChildMode#PERMANENT PERMANENET} child dies of any cause, or a
     * {@link Supervisor.ChildMode#TRANSIENT TRANSIENT} child dies an unnatural death (caused by an exception).
     */
    public enum RestartStrategy {
        /**
         * Kill the supervisor along with all children.
         */
        ESCALATE {
                    @Override
                    boolean onChildDeath(SupervisorActor supervisor, ChildEntry child, Throwable cause) throws SuspendExecution, InterruptedException {
                        if (child.spec.mode == Supervisor.ChildMode.TEMPORARY) {
                            supervisor.tryRestart(child, cause, supervisor.now(), null, true);
                            return true;
                        } else if (child.spec.mode == Supervisor.ChildMode.TRANSIENT) {
                            return cause != null
                                   || supervisor.tryRestart(child, cause, supervisor.now(), null, true);
                        } else {
                            return false;
                        }
                    }
                },
        /**
         * Restart the dead actor.
         */
        ONE_FOR_ONE {
                    @Override
                    boolean onChildDeath(SupervisorActor supervisor, ChildEntry child, Throwable cause) throws InterruptedException, SuspendExecution {
                        return supervisor.tryRestart(child, cause, supervisor.now(), null, true);
                    }
                },
        /**
         * Kill all surviving children, and restart them all.
         */
        ALL_FOR_ONE {
                    @Override
                    boolean onChildDeath(SupervisorActor supervisor, ChildEntry child, Throwable cause) throws SuspendExecution, InterruptedException {
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
        /**
         * Kill all children that were added to the supervisor <i>after</i> the addition of the dead actor, and restart them all
         * (including the actor whose death triggered the strategy).
         */
        REST_FOR_ONE {
                    @Override
                    boolean onChildDeath(SupervisorActor supervisor, ChildEntry child, Throwable cause) throws InterruptedException, SuspendExecution {
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

        abstract boolean onChildDeath(SupervisorActor supervisor, ChildEntry child, Throwable cause) throws SuspendExecution, InterruptedException;
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
