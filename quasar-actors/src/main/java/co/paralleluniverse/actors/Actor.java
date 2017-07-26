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
import co.paralleluniverse.concurrent.util.ThreadAccess;
import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.FiberWriter;
import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.io.serialization.ByteArraySerializer;
import co.paralleluniverse.io.serialization.Serialization;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactory;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.ReceivePort;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

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
    private transient volatile ActorRef<Message> wrapperRef;
    private transient /*final*/ AtomicReference<Class<?>> classRef;
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(MapUtil.<LifecycleListener, Boolean>newConcurrentHashMap());
    private final Set<ActorRef> observed = Collections.newSetFromMap(MapUtil.<ActorRef, Boolean>newConcurrentHashMap());
    private volatile V result;
    private volatile Throwable exception;
    private volatile Throwable deathCause;
    private volatile Object globalId;
    private transient volatile ActorMonitor monitor;
    private volatile boolean registered;
    private boolean hasMonitor;
    private ActorSpec<?, Message, V> spec;
    private Object aux;
    private /*final*/ ActorRunner<V> runner;
    private boolean migrating;
    private static final AtomicReferenceFieldUpdater<Actor, ActorRef> wrapperRefUpdater = AtomicReferenceFieldUpdater.newUpdater(Actor.class, ActorRef.class, "wrapperRef");
    private boolean forwardWatch;
    
    /**
     * Creates a new actor.
     *
     * @param name          the actor name (may be {@code null}).
     * @param mailboxConfig the actor's mailbox settings; if {@code null}, the default config - unbounded mailbox - will be used.
     */
    @SuppressWarnings({"OverridableMethodCallInConstructor", "LeakingThisInConstructor"})
    public Actor(String name, MailboxConfig mailboxConfig) {
        super(name, new Mailbox(mailboxConfig), new ActorRef<Message>());
        mailbox().setActor(this);

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
        this.runner = new ActorRunner<>(ref);
        this.classRef = ActorLoader.getClassRef(getClass());
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
    
    /**
     * <b>For use by non-Java, untyped languages only.</b>
     * <p>
     * If set to {@code true}, {@link #handleLifecycleMessage(co.paralleluniverse.actors.LifecycleMessage) LifecycleMessage}
     * will, by default, return {@link ExitMessage}s from {@link #watch(co.paralleluniverse.actors.ActorRef) watched} 
     * actors to be returned by {@code receive}. This means that {@code receive} will return a message of
     * a type that may not be {@code Message}, and therefore this value should only be set to true in
     * untyped languages.
     * 
     * @param value
     * @return 
     */
    @Deprecated
    public Actor<Message, V> setForwardWatch(boolean value) {
        this.forwardWatch = value;
        return this;
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
     * @param ff the {@link FiberFactory factory} (or {@link FiberScheduler scheduler}) that will be used to create the actor's fiber.
     * @return This actors' ActorRef
     */
    public ActorRef<Message> spawn(StrandFactory sf) {
        if (sf == null)
            return spawn();
        checkReplacement();
        final Strand s = sf.newStrand(runner);
        setStrand(s);
        if (getName() != null)
            s.setName(getName());
        s.start();
        return ref();
    }

    /**
     * Starts a new fiber using the given scheduler and runs the actor in it.
     * The fiber's name will be set to this actor's name.
     *
     * @param ff the {@link FiberFactory factory} (or {@link FiberScheduler scheduler}) that will be used to create the actor's fiber.
     * @return This actors' ActorRef
     */
    public ActorRef<Message> spawn(FiberFactory ff) {
        if (ff == null)
            return spawn();
        checkReplacement();
        ff.newFiber(runner).setName(getName()).start();
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
        final Fiber f = getName() != null ? new Fiber(getName(), runner) : new Fiber(runner);
        f.start();
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
    protected final void interrupt() {
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
        return ref0();
    }

    ActorRef<Message> ref0() {
        if (wrapperRef == null)
            wrapperRefUpdater.compareAndSet(this, null, makeRef(ref));
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
                checkThrownIn0();
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
            checkThrownIn0();
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
                checkThrownIn0();
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
            checkThrownIn0();
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
            checkThrownIn0();
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
    @Suspendable
    public final void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        runner.join(timeout, unit);
    }

    /**
     * Tests whether this actor has been started, i.e. whether the strand executing it has been started.
     */
    public final boolean isStarted() {
        return runner == null || runner.isStarted(); // runner == null iff migrateAndRestart
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

    protected final void verifyOnActorStrand() {
        if (!Strand.currentStrand().equals(getStrand()))
            throw new ConcurrencyException("Operation not called from within the actor's strand (" + getStrand() + ", but called in " + Strand.currentStrand() + ")");
    }

    /**
     * Tests whether this code is executing in this actor's strand.
     */
    protected final boolean isInActor() {
        return (currentActor() == this);
    }

    /**
     * Returns the actor associated with the given strand, or {@code null} if none is.
     */
    public static Actor getActor(Strand s) {
        final ActorRunner runner;
        if (s.isFiber())
            runner = (ActorRunner) ((Fiber) s.getUnderlying()).getTarget();
        else
            runner = (ActorRunner) Strand.unwrapSuspendable(ThreadAccess.getTarget((Thread) s.getUnderlying()));
        if (runner == null)
            return null;
        return runner.getActor();
    }
    //</editor-fold>

    //<editor-fold desc="Lifecycle">
    /////////// Lifecycle ///////////////////////////////////
    final V run0() throws InterruptedException, SuspendExecution {
        JMXActorsMonitor.getInstance().actorStarted(ref);
        final Strand strand = runner.getStrand(); // runner might be nulled by running actor
        if (!strand.isFiber())
            currentActor.set(this);
        try {
            if (this instanceof MigratingActor && globalId == null)
                this.globalId = MigrationService.registerMigratingActor();

            result = doRun();
            die(null);
            return result;
        } catch (ActorAbort abort) {
            throw abort;
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
            if (!strand.isFiber())
                currentActor.set(null);
            JMXActorsMonitor.getInstance().actorTerminated(ref, strand);
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
            if (!observed.remove(exit.getActor()))
                return null;
            if (exit.getWatch() == null) {
                throw new LifecycleException(m);
            } else if (forwardWatch)
                return (Message) m; // this is a false cast! forwardWatch must only be used in untyped languages
        }
        return null;
    }

    /**
     * Tests whether this actor has been upgraded via hot code-swapping.
     * If a new version of this actor is found, this method never returns
     * (a special {@code Error} is thrown which causes the actor to restart).
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
        return registered;
    }

    Object getGlobalId() {
        return globalId;
    }

    void setGlobalId(Object globalId) {
        this.globalId = globalId;
    }

    @Override
    public final void throwIn(RuntimeException e) {
        record(1, "Actor", "throwIn", "Exception %s thrown into actor %s", e, this);
        this.exception = e; // last exception thrown in wins
        runner.getStrand().interrupt();
    }

    /**
     * Tests whether an exception has been {@link #throwIn(RuntimeException) thrown into} this actor, and if so, throws it.
     * This method must only be called within the actor's strand.
     */
    public final void checkThrownIn() {
        verifyOnActorStrand();
        checkThrownIn0();
    }

    final void checkThrownIn0() {
        if (exception != null) {
            if (isRecordingLevel(1))
                record(1, "Actor", "checkThrownIn", "%s detected thrown in exception %s - %s", this, exception, Arrays.toString(exception.getStackTrace()));
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

        this.linked(other);
        other1.linked(myRef());
        
        return this;
    }

    @Override
    protected void linked(ActorRef actor) {
        if (!this.isDone())
            observed.add(actor);
        addLifecycleListener(getActorRefImpl(actor).getLifecycleListener());
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
        
        observed.remove(other);
        removeLifecycleListener(other1.getLifecycleListener());
        
        other1.unlinked(myRef());
        return this;
    }
    
    @Override
    protected void unlinked(ActorRef actor) {
        observed.remove(actor);
        removeLifecycleListener(getActorRefImpl(actor).getLifecycleListener());
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
        observed.add(other);
        other1.addLifecycleListener(listener);
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
        observed.remove(other);
        other1.removeLifecycleListener(listener);
    }

    /**
     * Registers this actor in the actor registry under the given name and sets this actor's name.
     * This also creates a {@link #monitor() monitor} for this actor.
     *
     * @param name the name of the actor in the registry, must be equal to the {@link #getName() actor's name} if it has one.
     * @return {@code this}
     */
    public final Actor<Message, V> register(String name) throws SuspendExecution {
        if (getName() == null)
            setName(name);
        else if (!getName().equals(name))
            throw new RegistrationException("Cannot register actor named " + getName() + " under a different name (" + name + ")");
        return register();
    }

    // called by ActorRegistry
    void preRegister(String name) throws SuspendExecution {
        if (getName() == null)
            setName(name);
        else if (!getName().equals(name))
            throw new RegistrationException("Cannot register actor named " + getName() + " under a different name (" + name + ")");
        assert !registered;
        if (this instanceof MigratingActor && globalId == null)
            this.globalId = MigrationService.registerMigratingActor();
    }

    void postRegister() {
        this.registered = true;
    }

    /**
     * Registers this actor in the actor registry under its name.
     * This also creates a {@link #monitor() monitor} for this actor.
     *
     * @return {@code this}
     */
    public final Actor register() throws SuspendExecution {
        if (registered)
            return this;
        record(1, "Actor", "register", "Registering actor %s as %s", this, getName());
        ActorRegistry.register(this);
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
        ActorRegistry.unregister(this);
        stopMonitor();
        this.registered = false;
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
        }

        // avoid memory leaks:
        lifecycleListeners.clear();
        for (ActorRef a : observed)
            getActorRefImpl(a).removeObserverListeners(myRef());
        observed.clear();
    }

    boolean isMigrating() {
        return migrating;
    }

    /**
     * Suspends and migrates the actor in such a way that when it is later hired, the actor is restarted
     * (i.e., its `doRun` method will be called again and run from the top), but the current value of the actor's fields will be preserved.
     * This method never returns.
     */
    public void migrateAndRestart() throws SuspendExecution {
        record(1, "Actor", "migrateAndRestart", "Actor %s is migrating.", this);
        verifyOnActorStrand();

        this.runner = null;
        migrating = true;
        try {
            preMigrate();
            MigrationService.migrate(getGlobalId(), this, Serialization.getInstance().write(this));
            postMigrate();
            throw Migrate.MIGRATE;
        } finally {
            migrating = false;
        }
    }

    /**
     * Suspends and migrate the actor.
     * This method suspends the fiber the actor is running in (and is therefore available only for actors running in fibers),
     * so that when the actor is hired, it will continue execution from the point this method was called.
     * This method must be called on a fiber.
     */
    public void migrate() throws SuspendExecution {
        record(1, "Actor", "migrate", "Actor %s is migrating.", this);
        verifyOnActorStrand();

        migrating = true;
        preMigrate();
        Fiber.parkAndSerialize(new FiberWriter() {

            @Override
            public void write(Fiber fiber, ByteArraySerializer ser) {
                final byte[] buf = ser.write(Actor.this);
                new Fiber<Void>() {
                    @Override
                    protected Void run() throws SuspendExecution, InterruptedException {
                        MigrationService.migrate(getGlobalId(), Actor.this, buf);
                        postMigrate();
                        return null;
                    }
                }.start();
            }
        });
        migrating = false;
    }

    private void preMigrate() {
        if (monitor != null) {
            hasMonitor = true;
            stopMonitor();
        }
        // must be done before migration because this sets up a local listener (which will be removed when migrating)
        ref.setImpl(RemoteActorProxyFactoryService.create(ref, getGlobalId()));
    }

    private void postMigrate() {
        assert ref.getImpl() instanceof RemoteActor;

        // copy messages already in the mailbox
        // TODO: this might change the message order, as new messages are coming in
        final Mailbox mbox = mailbox();
        for (;;) {
            Object m = mbox.tryReceive();
            if (m == null)
                break;
            // System.out.println("XXXXXXX ---> " + m);
            ref.getImpl().internalSendNonSuspendable(m);
        }
    }

    /**
     * Hires and resumes/restarts a migrated actor.
     *
     * @param ref the {@link ActorRef} of the migrated actor.
     * @return the ref
     */
    public static <M> ActorRef<M> hire(ActorRef<M> ref) throws SuspendExecution {
        return hire(ref, DefaultFiberScheduler.getInstance());
    }

    /**
     * Hires and resumes/restarts a migrated actor.
     *
     * @param ref       the {@link ActorRef} of the migrated actor.
     * @param scheduler the {@link FiberScheduler} on which to schedule this actor,
     *                  or {@code null} to schedule the actor on a thread.
     * @return the ref
     */
    public static <M> ActorRef<M> hire(ActorRef<M> ref, FiberScheduler scheduler) throws SuspendExecution {
        Actor actor = MigrationService.hire(ref, Fiber.getFiberSerializer());

        final Fiber<?> fiber = actor.runner != null ? (Fiber) actor.getStrand() : null;
        actor.setRef(ref);
        if (fiber == null)
            actor.runner = new ActorRunner<>(ref);
        // actor.runner = fiber != null ? (ActorRunner) fiber.getTarget() : new ActorRunner<>(ref);

        actor.ref.setImpl(actor);
        assert ref == actor.ref : ref + " - " + actor.ref;

        if (fiber != null)
            Fiber.unparkDeserialized(fiber, scheduler);
        else {
            if (scheduler != null) {
                final FiberFactory ff = scheduler;
                actor.spawn(ff);
            } else
                actor.spawnThread();
        }
        return ref;
    }
    //</editor-fold>

    //<editor-fold desc="ActorBuilder">
    /////////// ActorBuilder ///////////////////////////////////
    @Override
    public final Actor<Message, V> build() throws SuspendExecution {
        if (!isDone())
            throw new IllegalStateException("Actor " + this + " isn't dead. Cannot build a copy");

        final Actor newInstance = reinstantiate();

        if (newInstance.getName() == null)
            newInstance.setName(getName());
        newInstance.setStrand(null);

        newInstance.setMonitor(getMonitor());
        if (getName() != null && ref0() == ActorRegistry.tryGetActor(getName()))
            newInstance.register();
        return newInstance;
    }
    //</editor-fold>

    //<editor-fold desc="Serialization">
    /////////// Serialization ///////////////////////////////////
    protected final Object writeReplace() throws java.io.ObjectStreamException {
        if (migrating)
            return this;
        final RemoteActor<Message> remote = RemoteActorProxyFactoryService.create(ref(), getGlobalId());
        // remote.startReceiver();
        return remote;
    }

    protected Object readResolve() throws java.io.ObjectStreamException {
        this.classRef = ActorLoader.getClassRef(getClass());
        mailbox().setActor(this);
        if (hasMonitor)
            monitor();
        return this;
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
        final String name = getName().replaceAll(":", "");
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
