/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.actors;

import co.paralleluniverse.actors.ActorImpl.ActorLifecycleListener;
import static co.paralleluniverse.actors.ActorImpl.getActorRefImpl;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Objects;
import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.ReceivePort;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An actor is a self-contained execution unit - an object running in its own strand and communicating with other actors via messages.
 * An actor has a channel used as a mailbox, and can be monitored for errors.
 *
 * @param <Message> The message type the actor can receive. It is often {@link Object}.
 * @param <V>       The actor's return value type. Use {@link Void} if the actor does not return a result.
 * @author pron
 */
public abstract class Actor<Message, V> extends ActorImpl<Message> implements SuspendableCallable<V>, ActorBuilder<Message, V>, Joinable<V>, Stranded, ReceivePort<Message> {
    /**
     * Creates a new actor.
     * The actor must have a public constructor that can take the given parameters.
     *
     * @param <T>       The actor's type
     * @param <Message> The actor's message type.
     * @param <V>       The actor's return value type.
     * @param clazz     The actor's class
     * @param params    Parameters that will be passed to the actor class's constructor in order to construct a new instance.
     * @return A new actor of type T.
     */
    public static <T extends Actor<Message, V>, Message, V> T newActor(Class<T> clazz, Object... params) {
        return newActor(ActorSpec.of(clazz, params));
    }

    /**
     * Creates a new actor from an {@link ActorSpec}.
     *
     * @param <T>       The actor's type
     * @param <Message> The actor's message type.
     * @param <V>       The actor's return value type.
     * @param spec      The ActorSpec that defines how to build the actor.
     * @return A new actor of type T.
     */
    public static <T extends Actor<Message, V>, Message, V> T newActor(ActorSpec<T, Message, V> spec) {
        return spec.build();
    }
    private static final Throwable NATURAL = new Throwable();
    private static final Object DEFUNCT = new Object();
    private static final ThreadLocal<Actor> currentActor = new ThreadLocal<Actor>();
    private ActorRef<Message> wrapperRef;
    private final AtomicReference<Class<?>> classRef;
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(MapUtil.<LifecycleListener, Boolean>newConcurrentHashMap());
    private final Set<ActorImpl> observed = Collections.newSetFromMap(MapUtil.<ActorImpl, Boolean>newConcurrentHashMap());
    private volatile V result;
    private volatile Throwable exception;
    private volatile Throwable deathCause;
    private volatile Object globalId;
    private volatile ActorMonitor monitor;
    private ActorSpec<?, Message, V> spec;
    private Object aux;
    private final ActorRunner<V> runner;

    /**
     * Creates a new actor.
     *
     * @param name          the actor name (may be {@code null}).
     * @param mailboxConfig the actor's mailbox settings; if {@code null}, the default config - unbounded mailbox - will be used.
     */
    @SuppressWarnings({"OverridableMethodCallInConstructor", "LeakingThisInConstructor"})
    public Actor(String name, MailboxConfig mailboxConfig) {
        super(name, new Mailbox(mailboxConfig), new ActorRef<Message>());
        // initialization order in this constructor matters because of replacement (code swap) instance constructor below

        this.runner = new ActorRunner<>(ref);
        this.classRef = ActorLoader.getClassRef(getClass());

        // we cannot checkReplacement() here because the actor is not fully constructed yet (we're in the middle of the subclass's constructor)
        ref.setImpl(this);
    }

    /**
     * This constructor must only be called by hot code-swap actors, and never, ever, called by application code.
     */
    protected Actor() {
        super(null, null, null);
        this.wrapperRef = null;
        this.runner = null;
        this.classRef = null;
    }

    private void checkReplacement() {
        Actor<Message, V> impl = ActorLoader.getReplacementFor(this);
        ref.setImpl(impl);
        if (impl != this)
            defunct();
    }

    /**
     * Creates a new actor.
     *
     * @param strand        the actor's strand
     * @param name          the actor name (may be {@code null}).
     * @param mailboxConfig the actor's mailbox settings; if {@code null}, the default config - unbounded mailbox - will be used.
     */
    protected Actor(Strand strand, String name, MailboxConfig mailboxConfig) {
        this(name, mailboxConfig);
        if (strand != null)
            runner.setStrand(strand);
    }

    void onCodeChange0() {
        ref.setImpl(this);
        record(1, "Actor", "onCodeChange", "%s", this);
        onCodeChange();
    }

    void defunct() {
        this.aux = DEFUNCT;
    }

    boolean isDefunct() {
        return aux == DEFUNCT;
    }

    protected ActorRef<Message> makeRef(ActorRef<Message> ref) {
        return ref;
    }

    /**
     * Returns this actor's name.
     */
    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * Sets this actor's name. The name does not have to be unique, and may be {@code null}
     *
     * @param name
     */
    @Override
    public void setName(String name) {
        super.setName(name);
    }

    private ActorRef myRef() {
        return ref;
    }

    /**
     * Starts a new fiber using the given scheduler and runs the actor in it.
     * The fiber's name will be set to this actor's name.
     *
     * @param scheduler The new fiber's scheduler.
     * @return This actors' ActorRef
     */
    public ActorRef<Message> spawn(FiberScheduler scheduler) {
        checkReplacement();
        new Fiber(getName(), scheduler, runner).start();
        return ref();
    }

    /**
     * Starts a new fiber and runs the actor in it.
     * The fiber's name will be set to this actor's name.
     *
     * @return This actors' ActorRef
     */
    public ActorRef<Message> spawn() {
        checkReplacement();
        new Fiber(getName(), runner).start();
        return ref();
    }

    /**
     * Starts a new thread and runs the actor in it.
     * The fiber's name will be set to this actor's name.
     *
     * @return This actors' ActorRef
     */
    public ActorRef<Message> spawnThread() {
        checkReplacement();
        Runnable runnable = Strand.toRunnable(runner);
        Thread t = (getName() != null ? new Thread(runnable, getName()) : new Thread(runnable));
        setStrand(Strand.of(t));
        t.start();
        return ref();
    }

    @Override
    public final V run() throws InterruptedException, SuspendExecution {
        checkReplacement();
        return runner.run();
    }

    /**
     * Returns a "clone" of this actor, used by a {@link co.paralleluniverse.actors.behaviors.Supervisor supervisor} to restart this actor if it dies.
     * <p/>
     * If this actor is supervised by a {@link co.paralleluniverse.actors.behaviors.Supervisor supervisor} and was not created with the
     * {@link #newActor(co.paralleluniverse.actors.ActorSpec) newActor} factory method, then this method should be overridden.
     *
     * @return A new LocalActor instance that's a clone of this.
     */
    protected Actor<Message, V> reinstantiate() {
        if (spec != null)
            return newActor(spec);
        else if (getClass().isAnonymousClass() && getClass().getSuperclass().equals(Actor.class))
            return newActor(createSpecForAnonymousClass());
        else
            throw new RuntimeException("Actor " + this + " cannot be reinstantiated");
    }

    private ActorSpec<Actor<Message, V>, Message, V> createSpecForAnonymousClass() {
        assert getClass().isAnonymousClass() && getClass().getSuperclass().equals(Actor.class);
        Constructor<Actor<Message, V>> ctor = (Constructor<Actor<Message, V>>) getClass().getDeclaredConstructors()[0];
        Object[] params = new Object[ctor.getParameterTypes().length];
        for (int i = 0; i < params.length; i++) {
            Class<?> type = ctor.getParameterTypes()[i];
            if (String.class.equals(type))
                params[i] = getName();
            if (Integer.TYPE.equals(type))
                params[i] = mailbox().capacity();
            else
                params[i] = type.isPrimitive() ? 0 : null;
        }
        return new ActorSpec<Actor<Message, V>, Message, V>(ctor, params);
    }

    void setSpec(ActorSpec<?, Message, V> spec) {
        this.spec = spec;
    }

    Object getAux() {
        return aux;
    }

    void setAux(Object aux) {
        verifyInActor();
        this.aux = aux;
    }

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        if (className.isEmpty())
            className = getClass().getName().substring(getClass().getPackage().getName().length() + 1);
        final Strand strand = runner.getStrand();
        final String strandName = (strand != null ? strand.getName() : "null"); // strand.getClass().getSimpleName() + '@' + strand.getId()
        return className + "@"
                + (getName() != null ? getName() : Integer.toHexString(System.identityHashCode(this)))
                + "[owner: " + strandName + ']';
    }

    private static String systemToStringWithSimpleName(Object obj) {
        return (obj == null ? "null" : obj.getClass().getSimpleName() + "@" + Objects.systemObjectId(obj));
    }

    /**
     * Interrupts the actor's strand.
     */
    @Override
    final void interrupt() {
        getStrand().interrupt();
    }

    /**
     * Returns the actor currently running in the current strand.
     */
    public static <M, V> Actor<M, V> currentActor() {
        final Fiber currentFiber = Fiber.currentFiber();
        if (currentFiber == null)
            return currentActor.get();
        final SuspendableCallable target = currentFiber.getTarget();
        if (target == null)
            return null;
        if (target instanceof Actor)
            return (Actor<M, V>) target;
        if (target instanceof ActorRunner)
            return (Actor<M, V>) ((ActorRunner<V>) target).getActor();
        return null;
    }

    /**
     * Returns the ActorRef to this actor, if it has been started.
     *
     * @return the ActorRef of this actor if it has been started, or {@code null} otherwise.
     */
    @Override
    public ActorRef<Message> ref() {
        if (!isStarted())
            throw new IllegalStateException("Actor has not been started");
        if (wrapperRef == null)
            this.wrapperRef = makeRef(ref);
        return wrapperRef;
    }

    /**
     * Returns the ActorRef to this actor, if it has been started.
     *
     * @return the ActorRef of this actor if it has been started, or {@code null} otherwise.
     */
    protected ActorRef<Message> self() {
        return ref();
    }

    @Override
    public final void setStrand(Strand strand) {
        runner.setStrand(strand);
    }

    void setStrand0(Strand strand) {
        mailbox().setStrand(strand);
    }

    @Override
    public final Strand getStrand() {
        return runner.getStrand();
    }

    //<editor-fold desc="Mailbox methods">
    /////////// Mailbox methods ///////////////////////////////////
    /**
     * Returns the number of messages currently waiting in the mailbox.
     */
    public final int getQueueLength() {
        return mailbox().getQueueLength();
    }

    /**
     * Returns this actor's mailbox channel.
     */
    @Override
    protected final Mailbox<Object> mailbox() {
        return (Mailbox<Object>) super.mailbox();
    }

    @Override
    protected void internalSend(Object message) {
        internalSendNonSuspendable(message);
    }

    @Override
    protected void internalSendNonSuspendable(Object message) {
        record(1, "Actor", "send", "Sending %s -> %s", message, this);
        if (Debug.isDebug() && flightRecorder != null && flightRecorder.get().recordsLevel(2))
            record(2, "Actor", "send", "%s queue %s", this, getQueueLength());
        if (mailbox().isOwnerAlive())
            mailbox().sendNonSuspendable(message);
        else
            record(1, "Actor", "send", "Message dropped. Owner not alive.");
    }

    @Override
    protected final void sendSync(Message message) throws SuspendExecution {
        record(1, "Actor", "sendSync", "Sending sync %s -> %s", message, this);
        if (Debug.isDebug() && flightRecorder != null && flightRecorder.get().recordsLevel(2))
            record(2, "Actor", "sendSync", "%s queue %s", this, getQueueLength());
        if (mailbox().isOwnerAlive())
            mailbox().sendSync(message);
        else
            record(1, "Actor", "sendSync", "Message dropped. Owner not alive.");
    }

    @Override
    protected final boolean trySend(Message message) {
        record(1, "Actor", "trySend", "Sending %s -> %s", message, this);
        if (Debug.isDebug() && flightRecorder != null && flightRecorder.get().recordsLevel(2))
            record(2, "Actor", "trySend", "%s queue %s", this, getQueueLength());
        if (mailbox().isOwnerAlive()) {
            if (mailbox().trySend(message))
                return true;
            record(1, "Actor", "trySend", "Message not sent. Mailbox is not ready.");
            return false;

        }
        record(1, "Actor", "trySend", "Message dropped. Owner not alive.");
        return true;
    }

    /**
     * Returns the next message from the mailbox. If no message is currently available, this method blocks until a message arrives.
     *
     * @return a message sent to this actor.
     * @throws InterruptedException
     */
    @Override
    public final Message receive() throws SuspendExecution, InterruptedException {
        try {
            for (;;) {
                checkThrownIn();
                record(1, "Actor", "receive", "%s waiting for a message", this);
                final Object m = mailbox().receive();
                record(1, "Actor", "receive", "Received %s <- %s", this, m);
                if (Debug.isDebug() && flightRecorder != null && flightRecorder.get().recordsLevel(2))
                    record(2, "Actor", "receive", "%s queue %s", this, getQueueLength());
                monitorAddMessage();
                Message msg = filterMessage(m);
                if (msg != null)
                    return msg;
            }
        } catch (InterruptedException e) {
            checkThrownIn();
            throw e;
        }
    }

    /**
     * Returns the next message from the mailbox. If no message is currently available, this method blocks until a message arrives,
     * but no longer than the given timeout.
     *
     * @param timeout the maximum duration to block waiting for a message.
     * @param unit    the time unit of the timeout.
     * @return a message sent to this actor, or {@code null} if the timeout has expired.
     * @throws InterruptedException
     */
    @Override
    public final Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (unit == null)
            return receive();
        if (timeout <= 0)
            return tryReceive();

        long left = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + left;

        try {
            for (;;) {
                if (flightRecorder != null)
                    record(1, "Actor", "receive", "%s waiting for a message. millis left: ", this, TimeUnit.MILLISECONDS.convert(left, TimeUnit.NANOSECONDS));
                checkThrownIn();
                final Object m = mailbox().receive(left, TimeUnit.NANOSECONDS);
                if (m == null)
                    left = -1; // timeout
                else {
                    record(1, "Actor", "receive", "Received %s <- %s", this, m);
                    monitorAddMessage();

                    Message msg = filterMessage(m);
                    if (msg != null)
                        return msg;
                    else
                        left = deadline - System.nanoTime();
                }

                if (left <= 0) {
                    record(1, "Actor", "receive", "%s timed out.", this);
                    return null;
                }
            }
        } catch (InterruptedException e) {
            checkThrownIn();
            throw e;
        }
    }

    /**
     * Returns the next message from the mailbox. If no message is currently available, this method blocks until a message arrives,
     * but no longer than the given timeout.
     *
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return a message sent to this actor, or {@code null} if the timeout has expired.
     * @throws InterruptedException
     */
    @Override
    public final Message receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    /**
     * Retrieves a message from the mailbox if one is available. This method never blocks.
     *
     * @return a message, or {@code null} if one is not immediately available.
     */
    @Override
    public final Message tryReceive() {
        for (;;) {
            checkThrownIn();
            Object m = mailbox().tryReceive();
            if (m == null)
                return null;
            record(1, "Actor", "tryReceive", "Received %s <- %s", this, m);
            monitorAddMessage();

            Message msg = filterMessage(m);
            if (msg != null)
                return msg;
        }
    }

    /**
     * All messages received from the mailbox are passed to this method. If this method returns a non-null value, this value will be returned
     * from the {@code receive} methods. If it returns {@code null}, then {@code receive} will keep waiting.
     * <p/>
     * By default, this message passes all {@link LifecycleMessage} messages to {@link #handleLifecycleMessage(LifecycleMessage) handleLifecycleMessage}, while
     * other messages are returned (and will be returned by {@code receive}.
     *
     * @param m the message
     */
    protected Message filterMessage(Object m) {
        if (m instanceof LifecycleMessage) {
            return handleLifecycleMessage((LifecycleMessage) m);
        }
        return (Message) m;
    }

    @Override
    public final boolean isClosed() {
        return mailbox().isClosed();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }
    //</editor-fold>

    //<editor-fold desc="Strand helpers">
    /////////// Strand helpers ///////////////////////////////////
    public final Actor<Message, V> start() {
        record(1, "Actor", "start", "Starting actor %s", this);
        runner.getStrand().start();
        return this;
    }

    V getResult() throws ExecutionException {
        if (exception == null)
            return result;
        else
            throw new ExecutionException(exception);
    }

    Throwable getDeathCause0() {
        return deathCause;
    }

    @Override
    public final V get() throws InterruptedException, ExecutionException {
        return runner.get();
    }

    @Override
    public final V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return runner.get(timeout, unit);
    }

    @Override
    @Suspendable
    public final void join() throws ExecutionException, InterruptedException {
        runner.join();
    }

    @Override
    public final void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        runner.join(timeout, unit);
    }

    /**
     * Tests whether this actor has been started, i.e. whether the strand executing it has been started.
     */
    public final boolean isStarted() {
        return runner.isStarted();
    }

    /**
     * Tests whether this actor has terminated.
     */
    @Override
    public final boolean isDone() {
        return runner.isDone();
    }

    /**
     * Tests whether this code is executing in this actor's strand, and throws a {@link ConcurrencyException} if not.
     *
     * @see #isInActor()
     */
    protected final void verifyInActor() {
        if (!isInActor())
            throw new ConcurrencyException("Operation not called from within the actor (" + this + ", but called in " + currentActor() + ")");
    }

    /**
     * Tests whether this code is executing in this actor's strand.
     */
    protected final boolean isInActor() {
        return (currentActor() == this);
    }
    //</editor-fold>

    //<editor-fold desc="Lifecycle">
    /////////// Lifecycle ///////////////////////////////////
    final V run0() throws InterruptedException, SuspendExecution {
        JMXActorsMonitor.getInstance().actorStarted(ref);
        if (!(runner.getStrand() instanceof Fiber))
            currentActor.set(this);
        try {
            if (this instanceof MigratingActor && globalId == null)
                this.globalId = MigrationService.registerMigratingActor();

            result = doRun();
            die(null);
            return result;
        } catch (CodeSwap cs) {
            throw cs;
        } catch (InterruptedException e) {
            if (this.exception != null) {
                die(exception);
                throw (RuntimeException) exception;
            }
            die(e);
            throw e;
        } catch (Throwable t) {
            if (t.getCause() instanceof InterruptedException) {
                InterruptedException ie = (InterruptedException) t.getCause();
                if (this.exception != null) {
                    die(exception);
                    throw (RuntimeException) exception;
                }
                die(ie);
                throw ie;
            }
            this.exception = t;
            die(t);
            throw t;
        } finally {
            record(1, "Actor", "die", "Actor %s is now dead of %s", this, getDeathCause());
            if (!(runner.getStrand() instanceof Fiber))
                currentActor.set(null);
            JMXActorsMonitor.getInstance().actorTerminated(ref);
        }
    }

    /**
     * An actor must implement this method, which contains the actor's logic. This method begins executing on the actor's
     * strand.
     * <p/>
     * Upon a hot code-swap, this method is re-executed, so it is this method's responsibility to check this actor's state
     * (which may not be blank after a code-swap) when it begins.
     *
     * @return The actor's return value, which can be obtained with {@link #get() }.
     * @throws InterruptedException
     * @throws SuspendExecution
     */
    protected abstract V doRun() throws InterruptedException, SuspendExecution;

    /**
     * This method is called by this class during a call to any of the {@code receive} methods if a {@link LifecycleMessage} is found in the mailbox.
     * By default, if the message is an {@link ExitMessage} and its {@link ExitMessage#getWatch() watch} is {@code null}, i.e. it's a result
     * of a {@link #link(ActorRef) link} rather than a {@link #watch(ActorRef) watch}, it will throw a {@link LifecycleException}, which will,
     * in turn, cause this exception to be thrown by the call to {@code receive}.
     *
     * This method is not allowed to block. If you want to block as a result of a lifecycle message, return the message from this method
     * (rather than returning {@code null}), and have it processed by the caller to {@code receive}.
     *
     * @param m the message
     * @return {@code null} if the message has been processed and should not be returned by {@code receive}
     */
    protected Message handleLifecycleMessage(LifecycleMessage m) {
        record(1, "Actor", "handleLifecycleMessage", "%s got LifecycleMessage %s", this, m);
        if (m instanceof ExitMessage) {
            ExitMessage exit = (ExitMessage) m;
            removeObserverListeners(exit.getActor());
            if (exit.getWatch() == null)
                throw new LifecycleException(m);
        }
        return null;
    }

    /**
     * Tests whether this actor has been upgraded via hot code-swapping.
     * If a new version of this actor is found, this method never returns.
     *
     * @throws SuspendExecution
     */
    protected void checkCodeSwap() throws SuspendExecution {
        if (classRef == null)
            return;
        verifyInActor();
        if (classRef.get() != getClass()) {
            record(1, "Actor", "checkCodeSwap", "Code swap detected for %s", this);
            throw CodeSwap.CODE_SWAP;
        }
    }

    /**
     * This method is called on an actor instance replacing an active instance via hot code-swapping.
     * When this method is called, the fields of the old instance have been shalZlow-copied to this instance,
     * but this instance has not yet started to run.
     * This method should initialize any relevant state not copied from the old instance.
     */
    protected void onCodeChange() {
    }

    @Override
    protected final void addLifecycleListener(LifecycleListener listener) {
        final Throwable cause = getDeathCause();
        if (isDone()) {
            listener.dead(ref, cause);
            return;
        }
        lifecycleListeners.add(listener);
        if (isDone())
            listener.dead(ref, cause);
    }

    @Override
    protected void removeLifecycleListener(LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    @Override
    protected void removeObserverListeners(ActorRef actor) {
        for (Iterator<LifecycleListener> it = lifecycleListeners.iterator(); it.hasNext();) {
            LifecycleListener lifecycleListener = it.next();
            if (lifecycleListener instanceof ActorLifecycleListener)
                if (((ActorLifecycleListener) lifecycleListener).getObserver().equals(actor))
                    it.remove();
        }
    }

    /**
     * Returns this actor's cause of death
     *
     * @return the {@link Throwable} that caused this actor's death, or {@code null} if it died by natural causes, or if it not dead.
     */
    protected final Throwable getDeathCause() {
        return deathCause == NATURAL ? null : deathCause;
    }

    /**
     * Tests whether this actor has been {@link #register() registered}.
     *
     * @return {@code true} if the actor is registered; {@code false} otherwise.
     */
    public final boolean isRegistered() {
        return globalId != null;
    }

    Object getGlobalId() {
        return globalId;
    }

    @Override
    public final void throwIn(RuntimeException e) {
        record(1, "Actor", "throwIn", "Exception %s thrown into actor %s", e, this);
        this.exception = e; // last exception thrown in wins
        runner.getStrand().interrupt();
    }

    final void checkThrownIn() {
        if (exception != null) {
            record(1, "Actor", "checkThrownIn", "%s detected thrown in exception %s", this, exception);
            exception.setStackTrace(new Throwable().getStackTrace());
            throw (RuntimeException) exception;
        }
    }

    /**
     * Links this actor to another.
     *
     * A link is symmetrical. When two actors are linked and one of them dies, the other receives an {@link ExitMessage}, that is
     * handled by {@link #handleLifecycleMessage(LifecycleMessage) handleLifecycleMessage}, which, be default, throws a {@link LifecycleException}
     * as a response. The exception will be thrown by any of the {@code receive} methods.
     *
     * @param other the other actor
     * @return {@code this}
     * @see #watch(ActorRef)
     * @see #unlink(ActorRef)
     */
    public final Actor link(ActorRef other) {
        final ActorImpl other1 = getActorRefImpl(other);
        record(1, "Actor", "link", "Linking actors %s, %s", this, other1);
        if (this.isDone()) {
            other1.getLifecycleListener().dead(ref, getDeathCause());
        } else {
            addLifecycleListener(other1.getLifecycleListener());
            other1.addLifecycleListener(this.getLifecycleListener());
        }
        return this;
    }

    /**
     * Un-links this actor from another. This operation is symmetric.
     *
     * @param other the other actor
     * @return {@code this}
     * @see #link(ActorRef)
     */
    public final Actor unlink(ActorRef other) {
        final ActorImpl other1 = getActorRefImpl(other);
        record(1, "Actor", "unlink", "Uninking actors %s, %s", this, other1);
        removeLifecycleListener(other1.getLifecycleListener());
        other1.removeLifecycleListener(this.getLifecycleListener());
        return this;
    }

    /**
     * Makes this actor watch another actor.
     *
     * When the other actor dies, this actor receives an {@link ExitMessage}, that is
     * handled by {@link #handleLifecycleMessage(LifecycleMessage) handleLifecycleMessage}. This message does not cause an exception to be thrown,
     * unlike the case where it is received as a result of a linked actor's death.
     * <p/>
     * Unlike a link, a watch is asymmetric, and it is also composable, namely, calling this method twice with the same argument would result in two different values
     * returned, and in an {@link ExitMessage} to be received twice.
     *
     * @param other the other actor
     * @return a {@code watchId} object that identifies this watch in messages, and used to remove the watch by the {@link #unwatch(ActorRef, Object) unwatch} method.
     * @see #link(ActorRef)
     * @see #unwatch(ActorRef, Object)
     */
    public final Object watch(ActorRef other) {
        final Object id = ActorUtil.randtag();
        final LifecycleListener listener = new ActorLifecycleListener(myRef(), id);
        record(1, "Actor", "watch", "Actor %s to watch %s (listener: %s)", this, other, listener);
        final ActorImpl other1 = getActorRefImpl(other);
        other1.addLifecycleListener(listener);
        observed.add(other1);
        return id;
    }

    /**
     * Un-watches another actor.
     *
     * @param other   the other actor
     * @param watchId the object returned from the call to {@link #watch(ActorRef) watch(other)}
     * @see #watch(ActorRef)
     */
    public final void unwatch(ActorRef other, Object watchId) {
        final LifecycleListener listener = new ActorLifecycleListener(myRef(), watchId);
        record(1, "Actor", "unwatch", "Actor %s to stop watching %s (listener: %s)", this, other, listener);
        final ActorImpl other1 = getActorRefImpl(other);
        other1.removeLifecycleListener(listener);
        observed.remove(getActorRefImpl(other));
    }

    /**
     * Registers this actor in the actor registry under the given name and sets this actor's name.
     * This also creates a {@link #monitor() monitor} for this actor.
     *
     * @param name the name of the actor in the registry, must be equal to the {@link #getName() actor's name} if it has one.
     * @return {@code this}
     */
    public final Actor register(String name) {
        if (getName() == null)
            setName(name);
        else if (!getName().equals(name))
            throw new RegistrationException("Cannot register actor named " + getName() + " under a different name (" + name + ")");
        return register();
    }

    /**
     * Registers this actor in the actor registry under its name.
     * This also creates a {@link #monitor() monitor} for this actor.
     *
     * @return {@code this}
     */
    public final Actor register() {
        record(1, "Actor", "register", "Registering actor %s as %s", this, getName());
        this.globalId = ActorRegistry.register(this, globalId);
        return this;
    }

    /**
     * Unregisters this actor from the actor registry.
     *
     * @return {@code this}
     */
    public final Actor unregister() {
        if (!isRegistered())
            return this;
        record(1, "Actor", "unregister", "Unregistering actor %s (name: %s)", this, getName());
        if (getName() == null)
            throw new IllegalArgumentException("name is null");
        ActorRegistry.unregister(getName());
        if (monitor != null)
            this.monitor.setActor(null);
        this.globalId = null;
        return this;
    }

    /**
     * Called during this actor's death process.
     *
     * @param cause the cause of death; {@code null} for natural death.
     */
    private void die(Throwable cause) {
        record(1, "Actor", "die", "Actor %s is dying of cause %s", this, cause);
        this.deathCause = (cause == null ? NATURAL : cause);
        monitorAddDeath(cause);
        if (isRegistered())
            unregister();
        for (LifecycleListener listener : lifecycleListeners) {
            record(1, "Actor", "die", "Actor %s notifying listener %s of death.", this, listener);
            try {
                listener.dead(ref, cause);
            } catch (Exception e) {
                record(1, "Actor", "die", "Actor %s notifying listener %s of death failed with excetpion %s", this, listener, e);
            }

            // avoid memory leak in links:
            if (listener instanceof ActorLifecycleListener) {
                ActorLifecycleListener l = (ActorLifecycleListener) listener;
                if (l.getId() == null) // link
                    l.getObserver().getImpl().removeObserverListeners(myRef());
            }
        }

        // avoid memory leaks:
        lifecycleListeners.clear();
        for (ActorImpl a : observed)
            a.removeObserverListeners(myRef());
        observed.clear();
    }

    public void migrateAndRestart() {
        verifyInActor();

    }
    //</editor-fold>

    //<editor-fold desc="ActorBuilder">
    /////////// ActorBuilder ///////////////////////////////////
    @Override
    public final Actor<Message, V> build() {
        if (!isDone())
            throw new IllegalStateException("Actor " + this + " isn't dead. Cannot build a copy");

        final Actor newInstance = reinstantiate();

        if (newInstance.getName() == null)
            newInstance.setName(getName());
        newInstance.setStrand(null);

        ActorMonitor monitor = getMonitor();
        newInstance.setMonitor(monitor);
        if (getName() != null && ActorRegistry.getActor(getName()) == ref)
            newInstance.register();
        return newInstance;
    }
    //</editor-fold>

    //<editor-fold desc="Serialization">
    /////////// Serialization ///////////////////////////////////
    protected final Object writeReplace() throws java.io.ObjectStreamException {
        final RemoteActor<Message> repl = RemoteActorProxyFactoryService.create(ref(), getGlobalId());
        return repl;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Monitoring">
    /////////// Monitoring ///////////////////////////////////
    /**
     * Starts a monitor that exposes information about this actor via a JMX MBean.
     *
     * @return the monitor
     */
    public final ActorMonitor monitor() {
        if (monitor != null)
            return monitor;
        final String name = getName().toString().replaceAll(":", "");
        this.monitor = new JMXActorMonitor(name);
        monitor.setActor(ref);
        return monitor;
    }

    /**
     * Sets the actor's monitor
     *
     * @param monitor the monitor
     */
    public final void setMonitor(ActorMonitor monitor) {
        if (this.monitor == monitor)
            return;
        if (this.monitor != null)
            throw new RuntimeException("actor already has a monitor");
        this.monitor = monitor;
        monitor.setActor(ref);
    }

    /**
     * Shuts down the actor's monitor.
     */
    public final void stopMonitor() {
        if (monitor != null) {
            monitor.shutdown();
            this.monitor = null;
        }
    }

    public final ActorMonitor getMonitor() {
        return monitor;
    }

    protected final void monitorAddDeath(Object reason) {
        if (monitor != null)
            monitor.addDeath(reason);
    }

    protected final void monitorAddMessage() {
        if (monitor != null)
            monitor.addMessage();
    }

    protected final void monitorSkippedMessage() {
        if (monitor != null)
            monitor.skippedMessage();
    }

    protected final void monitorResetSkippedMessages() {
        if (monitor != null)
            monitor.resetSkippedMessages();
    }

    List<Object> getMailboxSnapshot() {
        return mailbox().getSnapshot();
    }

    StackTraceElement[] getStackTrace() {
        return runner.getStrand().getStackTrace();
    }
    //</editor-fold>
}
