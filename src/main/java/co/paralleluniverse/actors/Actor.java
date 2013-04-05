/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.channels.LwtObjectChannel;
import co.paralleluniverse.lwthreads.datastruct.QueueCapacityExceededException;
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
public abstract class Actor<Message, V> extends LightweightThread<V> {
    private static final Map<String, Actor> registeredActors = new ConcurrentHashMapV8<String, Actor>();
    private final LwtObjectChannel<Object> mailbox;
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(new ConcurrentHashMapV8<LifecycleListener, Boolean>());
    private volatile RuntimeException thrownIn;

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    @SuppressWarnings("LeakingThisInConstructor")
    public Actor(String name, ForkJoinPool fjPool, int stackSize, int mailboxSize) {
        super(name, fjPool, stackSize);
        this.mailbox = LwtObjectChannel.create(this, mailboxSize);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public Actor(String name, ForkJoinPool fjPool, int mailboxSize) {
        super(name, fjPool);
        this.mailbox = LwtObjectChannel.create(this, mailboxSize);
    }

    public Actor(ForkJoinPool fjPool, int stackSize, int mailboxSize) {
        this(null, fjPool, stackSize, mailboxSize);
    }

    public Actor(ForkJoinPool fjPool, int mailboxSize) {
        this(null, fjPool, mailboxSize);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public Actor(String name, int stackSize, int mailboxSize) {
        super(name, stackSize);
        this.mailbox = LwtObjectChannel.create(this, mailboxSize);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public Actor(String name, int mailboxSize) {
        super(name);
        this.mailbox = LwtObjectChannel.create(this, mailboxSize);
    }

    public Actor(int stackSize, int mailboxSize) {
        this((String) null, stackSize, mailboxSize);
    }

    public Actor(int mailboxSize) {
        this((String) null, mailboxSize);
    }
    //</editor-fold>

    //<editor-fold desc="Mailbox methods">
    /////////// Mailbox methods ///////////////////////////////////
    protected Message receive() throws SuspendExecution {
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
            public boolean process(Object message) {
                if (message instanceof LifecycleMessage) {
                    handleLifecycleMessage((LifecycleMessage) message);
                    return true;
                }
                return proc.process((Message) message);
            }
        };
    }

    protected Message receive(MessageProcessor<Message> proc, long timeout, TimeUnit unit, Message currentMessage) throws SuspendExecution {
        checkThrownIn();
        return (Message)mailbox.receive(wrapProcessor(proc), timeout, unit, currentMessage);
    }

    protected Message receive(MessageProcessor<Message> proc, Message currentMessage) throws SuspendExecution {
        return receive(proc, 0, null, currentMessage);
    }

    protected Message receive(MessageProcessor<Message> proc, long timeout, TimeUnit unit) throws SuspendExecution {
        return receive(proc, timeout, unit, null);
    }

    protected Message receive(MessageProcessor<Message> proc) throws SuspendExecution {
        return receive(proc, 0, null, null);
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

    //<editor-fold desc="Lifecycle">
    /////////// Lifecycle ///////////////////////////////////
    @Override
    protected abstract V run() throws SuspendExecution;

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
