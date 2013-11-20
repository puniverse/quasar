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
package co.paralleluniverse.actors;

import co.paralleluniverse.actors.ActorRefImpl.ActorLifecycleListener;
import static co.paralleluniverse.actors.ActorRefImpl.getActorRefImpl;
import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Objects;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.ReceivePort;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jsr166e.ConcurrentHashMapV8;

/**
 * An actor is a self-contained execution unit - an object running in its own strand and communicating with other actors via messages.
 * An actor has a channel used as a mailbox, and can be monitored for errors.
 *
 * @param <Message> The message type the actor can receive. It is often {@link Object}.
 * @param <V>       The actor's return value type. Use {@link Void} if the actor does not return a result.
 * @author pron
 */
public abstract class Actor<Message, V> implements SuspendableCallable<V>, Joinable<V>, Stranded, ReceivePort<Message> {
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
    private static final ThreadLocal<Actor> currentActor = new ThreadLocal<Actor>();
    private Strand strand;
    private final LocalActorRef<Message, V> ref;
    private final ActorRef<Message> wrapperRef;
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(new ConcurrentHashMapV8<LifecycleListener, Boolean>());
    private final Set<ActorRefImpl> observed = Collections.newSetFromMap(new ConcurrentHashMapV8<ActorRefImpl, Boolean>());
    private volatile V result;
    private volatile RuntimeException exception;
    private volatile Throwable deathCause;
    private volatile Object globalId;
    private volatile ActorMonitor monitor;
    private ActorSpec<?, Message, V> spec;
    private Object aux;
    protected transient final FlightRecorder flightRecorder;

    /**
     * Creates a new actor.
     *
     * @param name          The actor's name (may be {@code null}).
     * @param mailboxConfig Actor's mailbox settings.
     */
    @SuppressWarnings({"OverridableMethodCallInConstructor", "LeakingThisInConstructor"})
    public Actor(String name, MailboxConfig mailboxConfig) {
        this.ref = new LocalActorRef<Message, V>(this, name, new Mailbox(mailboxConfig));
        mailbox().setActor(this);
        this.flightRecorder = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;
        this.wrapperRef = makeRef(ref);
    }

    protected ActorRef<Message> makeRef(ActorRef<Message> ref) {
        return ref;
    }

    protected Actor(Strand strand, String name, MailboxConfig mailboxConfig) {
        this(name, mailboxConfig);
        if (strand != null)
            setStrand(strand);
    }

    /**
     * Returns this actor's name.
     */
    public String getName() {
        return ref.getName();
    }

    /**
     * Sets this actor's name. The name does not have to be unique, and may be {@code null}
     *
     * @param name
     */
    public void setName(String name) {
        myRef().setName(name);
    }

    private ActorRefImpl myRef() {
        return ((ActorRefImpl) ref);
    }

    /**
     * Starts a new fiber using the given scheduler and runs the actor in it.
     * The fiber's name will be set to this actor's name.
     *
     * @param scheduler The new fiber's scheduler.
     * @return This actors' ActorRef
     */
    public ActorRef<Message> spawn(FiberScheduler scheduler) {
        new Fiber(getName(), scheduler, this).start();
        return ref();
    }

    /**
     * Starts a new fiber and runs the actor in it.
     * The fiber's name will be set to this actor's name.
     *
     * @return This actors' ActorRef
     */
    public ActorRef<Message> spawn() {
        new Fiber(getName(), this).start();
        return ref();
    }

    /**
     * Starts a new thread and runs the actor in it.
     * The fiber's name will be set to this actor's name.
     *
     * @return This actors' ActorRef
     */
    public ActorRef<Message> spawnThread() {
        new Thread(Strand.toRunnable(this), getName()).start();
        return ref();
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
        return className + "@"
                + (getName() != null ? getName() : Integer.toHexString(System.identityHashCode(this)))
                + "[owner: " + systemToStringWithSimpleName(strand) + ']';
    }

    private static String systemToStringWithSimpleName(Object obj) {
        return (obj == null ? "null" : obj.getClass().getSimpleName() + "@" + Objects.systemObjectId(obj));
    }

    /**
     * Interrupts the actor's strand.
     */
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
        if (target == null || !(target instanceof Actor))
            return null;
        return (Actor<M, V>) target;
    }

    /**
     * Returns the ActorRef to this actor, if it has been started.
     *
     * @return the ActorRef of this actor if it has been started, or {@code null} otherwise.
     */
    public ActorRef<Message> ref() {
        if (!isStarted())
            throw new IllegalStateException("Actor has not been started");
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
        if (strand == this.strand)
            return;
        if (this.strand != null)
            throw new IllegalStateException("Strand already set to " + strand);
        this.strand = strand;
        if (strand != null && getName() == null)
            setName(strand.getName());
        mailbox().setStrand(strand);
    }

    @Override
    public final Strand getStrand() {
        return strand;
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
    protected final Mailbox<Object> mailbox() {
        return (Mailbox<Object>) ((ActorRefImpl) ref).mailbox();
    }

    void internalSend(Object message) {
        internalSendNonSuspendable(message);
    }

    void internalSendNonSuspendable(Object message) {
        record(1, "Actor", "send", "Sending %s -> %s", message, this);
        if (mailbox().isOwnerAlive())
            mailbox().sendNonSuspendable(message);
        else
            record(1, "Actor", "send", "Message dropped. Owner not alive.");
    }

    final void sendSync(Message message) throws SuspendExecution {
        record(1, "Actor", "sendSync", "Sending sync %s -> %s", message, this);
        if (mailbox().isOwnerAlive())
            mailbox().sendSync(message);
        else
            record(1, "Actor", "sendSync", "Message dropped. Owner not alive.");
    }

    boolean trySend(Message message) {
        record(1, "Actor", "trySend", "Sending %s -> %s", message, this);
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
        strand.start();
        return this;
    }

    @Override
    public final V get() throws InterruptedException, ExecutionException {
        if (strand instanceof Fiber)
            return ((Fiber<V>) strand).get();
        else {
            strand.join();
            return result;
        }
    }

    @Override
    public final V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (strand instanceof Fiber)
            return ((Fiber<V>) strand).get(timeout, unit);
        else {
            strand.join(timeout, unit);
            return result;
        }
    }

    @Override
    public final void join() throws ExecutionException, InterruptedException {
        strand.join();
    }

    @Override
    public final void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        strand.join(timeout, unit);
    }

    /**
     * Tests whether this actor has been started, i.e. whether the strand executing it has been started.
     */
    public final boolean isStarted() {
        return strand != null && strand.getState().compareTo(Strand.State.STARTED) >= 0;
    }

    /**
     * Tests whether this actor has terminated.
     */
    @Override
    public final boolean isDone() {
        return deathCause != null || strand.isTerminated();
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
    @Override
    public final V run() throws InterruptedException, SuspendExecution {
        if (strand == null)
            setStrand(Strand.currentStrand());
        JMXActorsMonitor.getInstance().actorStarted(ref);
        if (!(strand instanceof Fiber))
            currentActor.set(this);
        try {
            result = doRun();
            die(null);
            return result;
        } catch (InterruptedException e) {
            if (this.exception != null) {
                die(exception);
                throw exception;
            }
            die(e);
            throw e;
        } catch (Throwable t) {
            if (t.getCause() instanceof InterruptedException) {
                InterruptedException ie = (InterruptedException) t.getCause();
                if (this.exception != null) {
                    die(exception);
                    throw exception;
                }
                die(ie);
                throw ie;
            }
            die(t);
            throw t;
        } finally {
            record(1, "Actor", "die", "Actor %s is now dead of %s", this, getDeathCause());
            if (!(strand instanceof Fiber))
                currentActor.set(null);
            JMXActorsMonitor.getInstance().actorTerminated(ref);
        }
    }

    /**
     * An actor must implement this method, which contains the actor's logic. This method begins executing on the actor's
     * strand.
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
     * @param m the message
     * @return {@code null} if the message has been processed and should not be returned by {@code receive}
     */
    protected Message handleLifecycleMessage(LifecycleMessage m) {
        record(1, "Actor", "handleLifecycleMessage", "%s got LifecycleMessage %s", this, m);
        if (m instanceof ExitMessage) {
            ExitMessage exit = (ExitMessage) m;
            removeObserverListeners(getActorRefImpl(exit.getActor()));
            if (exit.getWatch() == null)
                throw new LifecycleException(m);
        }
        return null;
    }

    final void addLifecycleListener(LifecycleListener listener) {
        final Throwable cause = getDeathCause();
        if (isDone()) {
            listener.dead(ref, cause);
            return;
        }
        lifecycleListeners.add(listener);
        if (isDone())
            listener.dead(ref, cause);
    }

    void removeLifecycleListener(LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    void removeObserverListeners(ActorRef actor) {
        for (Iterator<LifecycleListener> it = lifecycleListeners.iterator(); it.hasNext();) {
            LifecycleListener lifecycleListener = it.next();
            if (lifecycleListener instanceof ActorLifecycleListener)
                if (((ActorLifecycleListener) lifecycleListener).getObserver().equals(actor))
                    it.remove();
        }
    }

    protected final Throwable getDeathCause() {
        return deathCause == NATURAL ? null : deathCause;
    }

    public final boolean isRegistered() {
        return globalId != null;
    }

    Object getGlobalId() {
        return globalId;
    }

    public final void throwIn(RuntimeException e) {
        record(1, "Actor", "throwIn", "Exception %s thrown into actor %s", e, this);
        this.exception = e; // last exception thrown in wins
        strand.interrupt();
    }

    final void checkThrownIn() {
        if (exception != null) {
            record(1, "Actor", "checkThrownIn", "%s detected thrown in exception %s", this, exception);
            exception.setStackTrace(new Throwable().getStackTrace());
            throw exception;
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
        final ActorRefImpl other1 = getActorRefImpl(other);
        record(1, "Actor", "link", "Linking actors %s, %s", this, other1);
        if (this.isDone()) {
            other1.getLifecycleListener().dead(ref, getDeathCause());
        } else {
            addLifecycleListener(other1.getLifecycleListener());
            other1.addLifecycleListener(myRef().getLifecycleListener());
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
        final ActorRefImpl other1 = getActorRefImpl(other);
        record(1, "Actor", "unlink", "Uninking actors %s, %s", this, other1);
        removeLifecycleListener(other1.getLifecycleListener());
        other1.removeLifecycleListener(myRef().getLifecycleListener());
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

        final ActorRefImpl other1 = getActorRefImpl(other);
        final LifecycleListener listener = new ActorLifecycleListener(ref, id);
        record(1, "Actor", "watch", "Actor %s to watch %s (listener: %s)", this, other1, listener);

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
        final ActorRefImpl other1 = getActorRefImpl(other);
        final LifecycleListener listener = new ActorLifecycleListener(ref, watchId);
        record(1, "Actor", "unwatch", "Actor %s to stop watching %s (listener: %s)", this, other1, listener);
        other1.removeLifecycleListener(listener);
        observed.remove(getActorRefImpl(other));
    }

    public final Actor register(String name) {
        if (getName() != null && !name.equals(name))
            throw new RegistrationException("Cannot register actor named " + getName() + " under a different name (" + name + ")");
        setName(name);
        return register();
    }

    public final Actor register() {
        record(1, "Actor", "register", "Registering actor %s as %s", this, getName());
        this.globalId = ActorRegistry.register(this);
        return this;
    }

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
                    l.getObserver().removeObserverListeners(myRef());
            }
        }

        // avoid memory leaks:
        lifecycleListeners.clear();
        for (ActorRefImpl a : observed)
            a.removeObserverListeners(myRef());
        observed.clear();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Monitoring">
    /////////// Monitoring ///////////////////////////////////
    public final ActorMonitor monitor() {
        if (monitor != null)
            return monitor;
        final String name = getName().toString().replaceAll(":", "");
        this.monitor = new JMXActorMonitor(name);
        monitor.setActor(ref);
        return monitor;
    }

    public final void setMonitor(ActorMonitor monitor) {
        if (this.monitor == monitor)
            return;
        if (this.monitor != null)
            throw new RuntimeException("actor already has a monitor");
        this.monitor = monitor;
        monitor.setActor(ref);
    }

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
        return strand.getStackTrace();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Recording">
    /////////// Recording ///////////////////////////////////
    protected final boolean isRecordingLevel(int level) {
        if (flightRecorder == null)
            return false;
        final FlightRecorder.ThreadRecorder recorder = flightRecorder.get();
        if (recorder == null)
            return false;
        return recorder.recordsLevel(level);
    }

    protected final void record(int level, String clazz, String method, String format) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4, arg5);
    }

    protected final void record(int level, String clazz, String method, String format, Object... args) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, args);
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, null));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object... args) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, args));
    }

    private static FlightRecorderMessage makeFlightRecorderMessage(FlightRecorder.ThreadRecorder recorder, String clazz, String method, String format, Object[] args) {
        return new FlightRecorderMessage(clazz, method, format, args);
        //return ((FlightRecorderMessageFactory) recorder.getAux()).makeFlightRecorderMessage(clazz, method, format, args);
    }
    //</editor-fold>
}
