/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.ObjectChannel;
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
public abstract class Actor<Message, V> implements SuspendableCallable<V>, Stranded {
    private static final Map<String, Actor> registeredActors = new ConcurrentHashMapV8<String, Actor>();
    private static final ThreadLocal<Actor> currentActor = new ThreadLocal<Actor>();
    private Strand strand;
    private String name;
    private ObjectChannel<Object> mailbox;
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(new ConcurrentHashMapV8<LifecycleListener, Boolean>());
    private volatile V result;
    private volatile RuntimeException exception;

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public Actor(String name, int mailboxSize) {
        this.mailbox = ObjectChannel.create(mailboxSize);
        this.name = name;
    }

    public Actor(int mailboxSize) {
        this((String) null, mailboxSize);
    }

    public Actor() {
        this((String) null, -1);
    }

    public Actor(Strand strand, String name, int mailboxSize) {
        this(name, mailboxSize);
        setStrand(strand);
    }

    public Actor(Strand strand, int mailboxSize) {
        this(strand, (String) null, mailboxSize);
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

    public SendChannel<Message> getMaibox() {
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
        for (;;) {
            checkThrownIn();
            Object m = mailbox.receive();
            if (m instanceof LifecycleMessage)
                handleLifecycleMessage((LifecycleMessage) m);
            else
                return (Message) m;
        }
    }

    private MessageProcessor<Object> wrapProcessor(final MessageProcessor<Message> proc) {
        return new MessageProcessor<Object>() {
            @Override
            public boolean process(Object message) throws SuspendExecution, InterruptedException {
                if (message instanceof LifecycleMessage) {
                    handleLifecycleMessage((LifecycleMessage) message);
                    return true;
                }
                return proc.process((Message) message);
            }
        };
    }

    protected Message receive(long timeout, TimeUnit unit, Message currentMessage, MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        checkThrownIn();
        return (Message) mailbox.receive(timeout, unit, currentMessage, wrapProcessor(proc));
    }

    protected Message receive(Message currentMessage, MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        return receive(0, null, currentMessage, proc);
    }

    protected Message receive(long timeout, TimeUnit unit, MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        return receive(timeout, unit, null, proc);
    }

    protected Message receive(MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        return receive(0, null, null, proc);
    }

    public void send(Message message) {
        try {
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

    public static Actor currentActor() {
        final Fiber currentFiber = Fiber.currentFiber();
        if (currentFiber == null)
            return currentActor.get();
        final SuspendableCallable target = currentFiber.getTarget();
        if (target == null || !(target instanceof Actor))
            return null;
        return (Actor) target;
    }

    //<editor-fold desc="Strand helpers">
    /////////// Strand helpers ///////////////////////////////////
    Actor<Message, V> start() {
        strand.start();
        return this;
    }

    public V get() throws InterruptedException, ExecutionException {
        if (strand instanceof Fiber)
            return ((Fiber<V>) strand).get();
        else {
            strand.join();
            return result;
        }
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (strand instanceof Fiber)
            return ((Fiber<V>) strand).get(timeout, unit);
        else {
            strand.join(timeout, unit);
            return result;
        }
    }

    public void join() throws ExecutionException, InterruptedException {
        strand.join();
    }

    public void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        strand.join(timeout, unit);
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
        if (m instanceof ExitMessage)
            throw new LifecycleException(m);
    }

    public String getName() {
        return name;
    }

    public void throwIn(RuntimeException e) {
        this.exception = e; // last exception thrown in wins
        strand.interrupt();
    }

    private void checkThrownIn() {
        if (exception != null) {
            exception.setStackTrace(new Throwable().getStackTrace());
            throw exception;
        }
    }

    public Actor register(String name) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        registeredActors.put(name, this);
        return this;
    }

    public Actor register() {
        return register(getName());
    }

    public static void unregister(String name) {
        registeredActors.remove(name);
    }

    public static Actor getActor(String name) {
        return registeredActors.get(name);
    }

    public Actor link(Actor other) {
        lifecycleListeners.add(other.lifecycleListener);
        other.lifecycleListeners.add(lifecycleListener);
        return this;
    }

    public Actor unlink(Actor other) {
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
        other.lifecycleListeners.add(listener);
        return listener;
    }

    public void demonitor(Actor other, Object listener) {
        other.lifecycleListeners.remove(listener);
    }

    private void notifyDeath(Object reason) {
        for (LifecycleListener listener : lifecycleListeners)
            listener.dead(this, reason);
    }
    //</editor-fold>
}
