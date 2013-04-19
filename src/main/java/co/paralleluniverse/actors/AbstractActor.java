/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

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
public abstract class AbstractActor<Message, V> implements SuspendableCallable<V>, Joinable<V>, Stranded {
    private static final Map<String, AbstractActor> registeredActors = new ConcurrentHashMapV8<String, AbstractActor>();
    private static final ThreadLocal<AbstractActor> currentActor = new ThreadLocal<AbstractActor>();
    private Strand strand;
    private String name;
    private final Mailbox<Object> mailbox;
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(new ConcurrentHashMapV8<LifecycleListener, Boolean>());
    private volatile V result;
    private volatile RuntimeException exception;

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public AbstractActor(String name, int mailboxSize) {
        this.name = name;
        this.mailbox = Mailbox.create(mailboxSize);
    }

    public AbstractActor(Strand strand, String name, int mailboxSize) {
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
    //</editor-fold>
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
            Object m = mailbox.receive();
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
            checkThrownIn();
            Object m = mailbox.receive(left, TimeUnit.NANOSECONDS);
            if (m instanceof LifecycleMessage)
                handleLifecycleMessage((LifecycleMessage) m);
            else
                return (Message) m;

            now = System.nanoTime();
            left = start + unit.toNanos(timeout) - now;
            if (left <= 0)
                return null;
        }
    }

    public void send(Message message) {
        try {
            if (mailbox.isOwnerAlive())
                mailbox.send(message);
        } catch (QueueCapacityExceededException e) {
            throwIn(e);
        }
    }

    public void sendSync(Message message) {
        try {
            mailbox.sendSync(message);
        } catch (QueueCapacityExceededException e) {
            throwIn(e);
        }
    }
    //</editor-fold>

    public static AbstractActor currentActor() {
        final Fiber currentFiber = Fiber.currentFiber();
        if (currentFiber == null)
            return currentActor.get();
        final SuspendableCallable target = currentFiber.getTarget();
        if (target == null || !(target instanceof AbstractActor))
            return null;
        return (AbstractActor) target;
    }

    //<editor-fold desc="Strand helpers">
    /////////// Strand helpers ///////////////////////////////////
    AbstractActor<Message, V> start() {
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
        if (m instanceof ExitMessage && ((ExitMessage) m).getMonitor() == null)
            throw new LifecycleException(m);
    }

    public String getName() {
        return name;
    }

    public void throwIn(RuntimeException e) {
        this.exception = e; // last exception thrown in wins
        strand.interrupt();
    }

    void checkThrownIn() {
        if (exception != null) {
            exception.setStackTrace(new Throwable().getStackTrace());
            throw exception;
        }
    }

    public AbstractActor register(String name) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        registeredActors.put(name, this);
        return this;
    }

    public AbstractActor register() {
        return register(getName());
    }

    public AbstractActor unregister() {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        unregister(name);
        return this;
    }

    public static void unregister(String name) {
        registeredActors.remove(name);
    }

    public static AbstractActor getActor(String name) {
        return registeredActors.get(name);
    }

    public AbstractActor link(AbstractActor other) {
        lifecycleListeners.add(other.lifecycleListener);
        other.lifecycleListeners.add(lifecycleListener);
        return this;
    }

    public AbstractActor unlink(AbstractActor other) {
        lifecycleListeners.remove(other.lifecycleListener);
        other.lifecycleListeners.remove(lifecycleListener);
        return this;
    }
    private final LifecycleListener lifecycleListener = new LifecycleListener() {
        @Override
        public void dead(AbstractActor actor, Object reason) {
            mailbox.send(new ExitMessage(actor, reason));
        }
    };

    public Object monitor(AbstractActor other) {
        LifecycleListener listener = new LifecycleListener() {
            @Override
            public void dead(AbstractActor actor, Object reason) {
                mailbox.send(new ExitMessage(actor, reason, this));
            }
        };
        other.lifecycleListeners.add(listener);
        return listener;
    }

    public void demonitor(AbstractActor other, Object listener) {
        other.lifecycleListeners.remove(listener);
    }

    private void notifyDeath(Object reason) {
        for (LifecycleListener listener : lifecycleListeners)
            listener.dead(this, reason);
    }
    //</editor-fold>
}
