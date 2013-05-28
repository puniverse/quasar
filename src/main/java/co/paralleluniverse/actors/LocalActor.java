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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.Mailbox;
import co.paralleluniverse.strands.channels.ReceiveChannel;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jsr166e.ConcurrentHashMapV8;

/**
 *
 * @author pron
 */
public abstract class LocalActor<Message, V> extends ActorImpl<Message> implements SuspendableCallable<V>, Joinable<V>, Stranded, ReceiveChannel<Message>, ActorBuilder<Message, V> {
    private static final ThreadLocal<LocalActor> currentActor = new ThreadLocal<LocalActor>();
    private Strand strand;
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(new ConcurrentHashMapV8<LifecycleListener, Boolean>());
    private volatile V result;
    private volatile RuntimeException exception;
    private volatile Throwable deathCause;
    private boolean registered;
    private ActorMonitor monitor;
    private ActorSpec<?, Message, V> spec;

    public LocalActor(String name, int mailboxSize) {
        super(name, Mailbox.create(mailboxSize));
    }

    public LocalActor(Strand strand, String name, int mailboxSize) {
        this(name, mailboxSize);
        if (strand != null)
            setStrand(strand);
    }

    public static <T extends LocalActor<Message, V>, Message, V> T newActor(Class<T> clazz, Object... params) {
        return newActor(new ActorSpec<T, Message, V>(clazz, params));
    }

    public static <T extends LocalActor<Message, V>, Message, V> T newActor(ActorSpec<T, Message, V> spec) {
        return spec.build();
    }

    @Override
    public final LocalActor<Message, V> build() {
        if (!isDone())
            throw new IllegalStateException("Actor " + this + " isn't dead. Cannot build a copy");

        final LocalActor newInstance = reinstantiate();

        newInstance.setName(this.getName());
        newInstance.strand = null;
        newInstance.monitor = this.monitor;
        monitor.setActor(newInstance);
        if (getName() != null && ActorRegistry.getActor(getName()) == this)
            newInstance.register();
        return newInstance;
    }

    protected LocalActor<Message, V> reinstantiate() {
        final LocalActor<Message, V> newInstance;
        if (spec != null)
            newInstance = newActor(spec);
        else if (getClass().isAnonymousClass() && getClass().getSuperclass().equals(LocalActor.class))
            newInstance = newActor(createSpecForAnonymousClass());
        else
            throw new RuntimeException("Actor " + this + " cannot be reinstantiated");
        return newInstance;
    }

    private ActorSpec<LocalActor<Message, V>, Message, V> createSpecForAnonymousClass() {
        assert getClass().isAnonymousClass() && getClass().getSuperclass().equals(LocalActor.class);
        Constructor<LocalActor<Message, V>> ctor = (Constructor<LocalActor<Message, V>>) getClass().getDeclaredConstructors()[0];
        Object[] params = new Object[ctor.getParameterTypes().length];
        for (int i = 0; i < params.length; i++) {
            Class<?> type = ctor.getParameterTypes()[i];
            if (String.class.equals(type))
                params[i] = getName();
            if (Integer.TYPE.equals(type))
                params[i] = mailbox.capacity();
            else
                params[i] = type.isPrimitive() ? 0 : null;
        }
        return new ActorSpec<LocalActor<Message, V>, Message, V>(ctor, params);
    }

    void setSpec(ActorSpec<?, Message, V> spec) {
        this.spec = spec;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + (getName() != null ? getName() : Integer.toHexString(System.identityHashCode(this))) + "[owner: " + strand + ']';
    }

    public ActorMonitor monitor() {
        if (monitor != null)
            return monitor;
        final String name = getName().toString().replaceAll(":", "");
        this.monitor = new JMXActorMonitor(name);
        monitor.setActor(this);
        return monitor;
    }

    public void stopMonitor() {
        monitor.shutdown();
        this.monitor = null;
    }

    public ActorMonitor getMonitor() {
        return monitor;
    }

    public static LocalActor currentActor() {
        final Fiber currentFiber = Fiber.currentFiber();
        if (currentFiber == null)
            return currentActor.get();
        final SuspendableCallable target = currentFiber.getTarget();
        if (target == null || !(target instanceof Actor))
            return null;
        return (LocalActor) target;
    }

    @Override
    public final void setStrand(Strand strand) {
        if (this.strand != null)
            throw new IllegalStateException("Strand already set to " + strand);
        this.strand = strand;
        if (getName() == null)
            setName(strand.getName());
        mailbox.setStrand(strand);
    }

    @Override
    public Strand getStrand() {
        return strand;
    }

    //<editor-fold desc="Mailbox methods">
    /////////// Mailbox methods ///////////////////////////////////
    public int getQueueLength() {
        return mailbox().getQueueLength();
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        for (;;) {
            checkThrownIn();
            record(1, "Actor", "receive", "%s waiting for a message", this);
            Object m = mailbox.receive();
            record(1, "Actor", "receive", "Received %s <- %s", this, m);
            monitorAddMessage();
            Message msg = filterMessage(m);
            if (msg != null)
                return msg;
        }
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (timeout <= 0 || unit == null)
            return receive();

        final long start = System.nanoTime();
        long now;
        long left = unit.toNanos(timeout);

        for (;;) {
            if (flightRecorder != null)
                record(1, "Actor", "receive", "%s waiting for a message. millis left: ", this, TimeUnit.MILLISECONDS.convert(left, TimeUnit.NANOSECONDS));
            checkThrownIn();
            Object m = mailbox.receive(left, TimeUnit.NANOSECONDS);
            if (m != null) {
                record(1, "Actor", "receive", "Received %s <- %s", this, m);
                monitorAddMessage();
            }

            Message msg = filterMessage(m);
            if (msg != null)
                return msg;

            now = System.nanoTime();
            left = start + unit.toNanos(timeout) - now;
            if (left <= 0) {
                record(1, "Actor", "receive", "%s timed out.", this);
                return null;
            }
        }
    }

    protected Message tryReceive() {
        for (;;) {
            checkThrownIn();
            Object m = mailbox.tryReceive();
            if (m == null)
                return null;
            record(1, "Actor", "tryReceive", "Received %s <- %s", this, m);
            monitorAddMessage();
            
            Message msg = filterMessage(m);
            if (msg != null)
                return msg;
        }
    }

    protected Message filterMessage(Object m) {
        if (m instanceof LifecycleMessage) {
            handleLifecycleMessage((LifecycleMessage) m);
            return null;
        }
        return (Message)m;
    }
    //</editor-fold>

    //<editor-fold desc="Strand helpers">
    /////////// Strand helpers ///////////////////////////////////
    public LocalActor<Message, V> start() {
        record(1, "Actor", "start", "Starting actor %s", this);
        strand.start();
        return this;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        if (strand instanceof Fiber)
            return ((Fiber<V>) strand).get();
        else {
            strand.join();
            return result;
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (strand instanceof Fiber)
            return ((Fiber<V>) strand).get(timeout, unit);
        else {
            strand.join(timeout, unit);
            return result;
        }
    }

    @Override
    public void join() throws ExecutionException, InterruptedException {
        strand.join();
    }

    @Override
    public void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        strand.join(timeout, unit);
    }

    @Override
    public boolean isDone() {
        return !strand.isAlive();
    }

    protected void verifyInActor() {
        if (currentActor() != this)
            throw new ConcurrencyException("Operation not called from within the actor (" + this + ")");
    }
    //</editor-fold>
    
    //<editor-fold desc="Lifecycle">
    /////////// Lifecycle ///////////////////////////////////
    @Override
    public final V run() throws InterruptedException, SuspendExecution {
        if (strand == null)
            setStrand(Strand.currentStrand());
        if (!(strand instanceof Fiber))
            currentActor.set(this);
        try {
            init();
            result = doRun();
            die(null);
            return result;
        } catch (InterruptedException e) {
            checkThrownIn();
            die(e);
            throw e;
        } catch (Throwable t) {
            die(t);
            throw t;
        } finally {
            if (!(strand instanceof Fiber))
                currentActor.set(null);
        }
    }

    protected void init() {
    }

    protected abstract V doRun() throws InterruptedException, SuspendExecution;

    protected void handleLifecycleMessage(LifecycleMessage m) {
        record(1, "Actor", "handleLifecycleMessage", "%s got LifecycleMessage %s", this, m);
        if (m instanceof ExitMessage && ((ExitMessage) m).getWatch() == null)
            throw new LifecycleException(m);
    }

    @Override
    LifecycleListener getLifecycleListener() {
        return lifecycleListener;
    }

    @Override
    void addLifecycleListener(LifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    @Override
    void removeLifecycleListener(LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    @Override
    Throwable getDeathCause() {
        return deathCause;
    }

    @Override
    public void throwIn(RuntimeException e) {
        record(1, "Actor", "throwIn", "Exception %s thrown into actor %s", e, this);
        this.exception = e; // last exception thrown in wins
        strand.interrupt();
    }

    void checkThrownIn() {
        if (exception != null) {
            record(1, "Actor", "checkThrownIn", "%s detected thrown in exception %s", this, exception);
            exception.setStackTrace(new Throwable().getStackTrace());
            throw exception;
        }
    }

    public Actor register(Object name) {
        if (getName() != null && !name.equals(name))
            throw new RegistrationException("Cannot register actor named " + getName() + " under a different name (" + name + ")");
        setName(name);
        return register();
    }

    public Actor register() {
        record(1, "Actor", "register", "Registering actor %s as %s", this, getName());
        ActorRegistry.register(this);
        this.registered = true;
        return this;
    }

    public Actor unregister() {
        record(1, "Actor", "unregister", "Unregistering actor %s (name: %s)", getName());
        if (getName() == null)
            throw new IllegalArgumentException("name is null");
        ActorRegistry.unregister(getName());
        this.monitor = null;
        this.registered = false;
        return this;
    }

    private void die(Throwable reason) {
        this.deathCause = reason;
        monitorAddDeath(reason);
        if (registered)
            unregister();
        for (LifecycleListener listener : lifecycleListeners)
            listener.dead(this, reason);
        lifecycleListeners.clear(); // avoid memory leak
    }
    private final LifecycleListener lifecycleListener = new LifecycleListener() {
        @Override
        public void dead(Actor actor, Throwable cause) {
            mailbox.send(new ExitMessage(actor, cause));
        }
    };
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Monitor delegates">
    /////////// Monitor delegates ///////////////////////////////////
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
    //</editor-fold>
}
