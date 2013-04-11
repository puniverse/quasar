/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.SuspendableCallable;
import co.paralleluniverse.channels.Channel;
import co.paralleluniverse.channels.ObjectChannel;
import co.paralleluniverse.channels.SendChannel;
import co.paralleluniverse.fibers.queues.QueueCapacityExceededException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jsr166e.ConcurrentHashMapV8;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public abstract class Actor<Message, V> extends Fiber<V> {
    private static final Map<String, Actor> registeredActors = new ConcurrentHashMapV8<String, Actor>();
    private final ObjectChannel<Object> mailbox;
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(new ConcurrentHashMapV8<LifecycleListener, Boolean>());
    private volatile RuntimeException thrownIn;

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    @SuppressWarnings("LeakingThisInConstructor")
    Actor(String name, ForkJoinPool fjPool, int stackSize, int mailboxSize, SuspendableCallable<V> target) {
        super(name, fjPool, stackSize, target);
        this.mailbox = ObjectChannel.create(this, mailboxSize);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    Actor(String name, int stackSize, int mailboxSize, SuspendableCallable<V> target) {
        super(name, stackSize, target);
        this.mailbox = ObjectChannel.create(this, mailboxSize);
    }

    public Actor(String name, ForkJoinPool fjPool, int stackSize, int mailboxSize) {
        this(name, fjPool, stackSize, mailboxSize, null);
    }

    public Actor(String name, ForkJoinPool fjPool, int mailboxSize) {
        this(name, fjPool, -1, mailboxSize, null);
    }

    public Actor(ForkJoinPool fjPool, int stackSize, int mailboxSize) {
        this(null, fjPool, stackSize, mailboxSize, null);
    }

    public Actor(ForkJoinPool fjPool, int mailboxSize) {
        this(null, fjPool, -1, mailboxSize, null);
    }

    public Actor(String name, int stackSize, int mailboxSize) {
        this(name, stackSize, mailboxSize, null);
    }

    public Actor(String name, int mailboxSize) {
        this(name, -1, mailboxSize, null);
    }

    public Actor(int stackSize, int mailboxSize) {
        this((String) null, stackSize, mailboxSize, null);
    }

    public Actor(int mailboxSize) {
        this((String) null, -1, mailboxSize, null);
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

    @Override
    public Actor start() {
        return (Actor) super.start();
    }
    
    public Actor currentActor() {
        return (Actor)currentFiber();
    }

    //<editor-fold desc="Lifecycle">
    /////////// Lifecycle ///////////////////////////////////
    @Override
    protected abstract V run() throws InterruptedException, SuspendExecution;

    protected void handleLifecycleMessage(LifecycleMessage m) {
        if (m instanceof ExitMessage)
            throw new LifecycleException(m);
    }

    @Override
    protected void onCompletion() {
        notifyDeath(null);
    }

    @Override
    protected void onException(Throwable t) {
        notifyDeath(t);
        Exceptions.rethrow(t);
    }

    @Override
    protected void postRestore() {
        super.postRestore();
        checkThrownIn();
    }

    public void throwIn(RuntimeException e) {
        this.thrownIn = e; // last exception thrown in wins
    }

    private void checkThrownIn() {
        if (thrownIn != null) {
            thrownIn.setStackTrace(new Throwable().getStackTrace());
            throw thrownIn;
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
