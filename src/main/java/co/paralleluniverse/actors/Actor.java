/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Mailbox;
import co.paralleluniverse.strands.channels.SendChannel;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jsr166e.ConcurrentHashMapV8;

/**
 *
 * @author pron
 */
public abstract class Actor<Message, V> implements SuspendableCallable<V>, Joinable<V>, Stranded, java.io.Serializable {
    static final long serialVersionUID = 894359345L;
    private static final Map<Object, Actor> registeredActors = new ConcurrentHashMapV8<Object, Actor>();
    private static final ThreadLocal<Actor> currentActor = new ThreadLocal<Actor>();
    private Strand strand;
    private String name;
    private final Mailbox<Object> mailbox;
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(new ConcurrentHashMapV8<LifecycleListener, Boolean>());
    private volatile V result;
    private volatile RuntimeException exception;
    protected final FlightRecorder flightRecorder;

    public Actor(String name, int mailboxSize) {
        this.name = name;
        this.mailbox = Mailbox.create(mailboxSize);

        if (Debug.isDebug())
            this.flightRecorder = Debug.getGlobalFlightRecorder();
        else
            this.flightRecorder = null;
    }

    public Actor(Strand strand, String name, int mailboxSize) {
        this(name, mailboxSize);
        setStrand(strand);
    }

    @Override
    public final void setStrand(Strand strand) {
        if (this.strand != null)
            throw new IllegalStateException("Strand already set to " + strand);
        this.strand = strand;
        this.name = (name != null ? name : strand.getName());
        mailbox.setStrand(strand);
    }

    @Override
    public Strand getStrand() {
        return strand;
    }

    @Override
    public String toString() {
        return "Actor@" + (name != null ? name : Integer.toHexString(System.identityHashCode(this))) + "[owner: " + strand + ']';
    }

    //<editor-fold desc="Mailbox methods">
    /////////// Mailbox methods ///////////////////////////////////
    Mailbox<Object> mailbox() {
        return mailbox;
    }

    public SendChannel<Message> getMailbox() {
        return (Channel<Message>) mailbox;
    }

    protected Message receive() throws SuspendExecution, InterruptedException {
        for (;;) {
            checkThrownIn();
            record(1, "Actor", "receive", "%s waiting for a message", this);
            Object m = mailbox.receive();
            record(1, "Actor", "receive", "Received %s <- %s", this, m);
            if (m instanceof LifecycleMessage)
                handleLifecycleMessage((LifecycleMessage) m);
            else
                return (Message) m;
        }
    }

    protected Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
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
            if (m instanceof LifecycleMessage)
                handleLifecycleMessage((LifecycleMessage) m);
            else
                return (Message) m;

            now = System.nanoTime();
            left = start + unit.toNanos(timeout) - now;
            if (left <= 0) {
                record(1, "Actor", "receive", "%s timed out.", this);
                return null;
            }
        }
    }

    public void send(Message message) {
        try {
            record(1, "Actor", "send", "Sending %s -> %s", message, this);
            if (mailbox.isOwnerAlive())
                mailbox.send(message);
            else
                record(1, "Actor", "send", "Message dropped. Owner not alive.");
        } catch (QueueCapacityExceededException e) {
            throwIn(e);
        }
    }

    public void sendSync(Message message) {
        try {
            record(1, "Actor", "sendSync", "Sending sync %s -> %s", message, this);
            if (mailbox.isOwnerAlive())
                mailbox.sendSync(message);
            else
                record(1, "Actor", "sendSync", "Message dropped. Owner not alive.");
        } catch (QueueCapacityExceededException e) {
            throwIn(e);
        }
    }
    //</editor-fold>

    public static Actor currentActor() {
        final Fiber currentFiber = Fiber.currentFiber();
        if (currentFiber == null)
            return currentActor.get();
        final SuspendableCallable target = currentFiber.getTarget();
        if (target == null || !(target instanceof Actor))
            return null;
        return (Actor) target;
    }

    //<editor-fold desc="Serialization">
    /////////// Serialization ///////////////////////////////////
    // If using Kryo, see what needs to be done: https://code.google.com/p/kryo/
    protected Object writeReplace() throws java.io.ObjectStreamException {
        //return new SerializedActor(this);
        throw new UnsupportedOperationException();
    }
    
    protected static class SerializedActor implements java.io.Serializable {
        static final long serialVersionUID = 894359345L;
        private Actor actor;

        public SerializedActor(Actor actor) {
            this.actor = actor;
        }

        public SerializedActor() {
        }
        
        protected Object readResolve() throws java.io.ObjectStreamException {
            // return new Actor(...);
            throw new UnsupportedOperationException();
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="Strand helpers">
    /////////// Strand helpers ///////////////////////////////////
    Actor<Message, V> start() {
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
        return strand.isAlive();
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
            result = doRun();
            notifyDeath(null);
            return result;
        } catch (InterruptedException e) {
            checkThrownIn();
            notifyDeath(e);
            throw e;
        } catch (Throwable t) {
            notifyDeath(t);
            throw t;
        } finally {
            if (!(strand instanceof Fiber))
                currentActor.set(this);
        }
    }

    protected abstract V doRun() throws InterruptedException, SuspendExecution;

    protected void handleLifecycleMessage(LifecycleMessage m) {
        record(1, "Actor", "handleLifecycleMessage", "%s got LifecycleMessage %s", this, m);
        if (m instanceof ExitMessage && ((ExitMessage) m).getMonitor() == null)
            throw new LifecycleException(m);
    }

    public String getName() {
        return name;
    }

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
        record(1, "Actor", "register", "Registering actor %s as %s", this, name);
        if (name == null)
            throw new IllegalArgumentException("name is null");
        registeredActors.put(name, this);
        return this;
    }

    public Actor register() {
        return register(getName());
    }

    public Actor unregister() {
        record(1, "Actor", "unregister", "Unregistering actor %s (name: %s)", name);
        if (name == null)
            throw new IllegalArgumentException("name is null");
        unregister(name);
        return this;
    }

    public static void unregister(Object name) {
        registeredActors.remove(name);
    }

    public static Actor getActor(Object name) {
        return registeredActors.get(name);
    }

    public Actor link(Actor other) {
        record(1, "Actor", "link", "Linking actors %s, %s", this, other);
        lifecycleListeners.add(other.lifecycleListener);
        other.lifecycleListeners.add(lifecycleListener);
        return this;
    }

    public Actor unlink(Actor other) {
        record(1, "Actor", "unlink", "Uninking actors %s, %s", this, other);
        lifecycleListeners.remove(other.lifecycleListener);
        other.lifecycleListeners.remove(lifecycleListener);
        return this;
    }
    private final LifecycleListener lifecycleListener = new LifecycleListener() {
        @Override
        public void dead(Actor actor, Object reason) {
            mailbox.send(new ExitMessage(actor, reason));
        }
    };

    public Object monitor(Actor other) {
        LifecycleListener listener = new LifecycleListener() {
            @Override
            public void dead(Actor actor, Object reason) {
                mailbox.send(new ExitMessage(actor, reason, this));
            }
        };
        record(1, "Actor", "monitor", "Actor %s to monitor %s (listener: %s)", this, other, listener);
        other.lifecycleListeners.add(listener);
        return listener;
    }

    public void demonitor(Actor other, Object listener) {
        record(1, "Actor", "demonitor", "Actor %s to stop monitoring %s (listener: %s)", this, other, listener);
        other.lifecycleListeners.remove(listener);
    }

    private void notifyDeath(Object reason) {
        for (LifecycleListener listener : lifecycleListeners)
            listener.dead(this, reason);
    }
    //</editor-fold>

    //<editor-fold desc="Recording">
    /////////// Recording ///////////////////////////////////
    protected void record(int level, String clazz, String method, String format) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format);
    }

    protected void record(int level, String clazz, String method, String format, Object arg1) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1);
    }

    protected void record(int level, String clazz, String method, String format, Object arg1, Object arg2) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2);
    }

    protected void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3);
    }

    protected void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4);
    }

    protected void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4, arg5);
    }

    protected void record(int level, String clazz, String method, String format, Object... args) {
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
