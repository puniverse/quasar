/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jsr166e.ConcurrentHashMapV8;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public abstract class Actor<Message> extends LightweightThread {
    private final Mailbox<Object, ?> mailbox;
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(new ConcurrentHashMapV8<LifecycleListener, Boolean>());

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    @SuppressWarnings("LeakingThisInConstructor")
    public Actor(String name, ForkJoinPool fjPool, int stackSize, int mailboxSize) {
        super(name, fjPool, stackSize);
        this.mailbox = Mailbox.createMailbox(this, mailboxSize);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public Actor(String name, ForkJoinPool fjPool, int mailboxSize) {
        super(name, fjPool);
        this.mailbox = Mailbox.createMailbox(this, mailboxSize);
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
        this.mailbox = Mailbox.createMailbox(this, mailboxSize);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public Actor(String name, int mailboxSize) {
        super(name);
        this.mailbox = Mailbox.createMailbox(this, mailboxSize);
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

    protected void receive(MessageProcessor<Message> proc, long timeout, TimeUnit unit, Message currentMessage) throws SuspendExecution {
        mailbox.receive(wrapProcessor(proc), timeout, unit, currentMessage);
    }

    protected void receive(MessageProcessor<Message> proc, Message currentMessage) throws SuspendExecution {
        mailbox.receive(wrapProcessor(proc), currentMessage);
    }

    protected void receive(MessageProcessor<Message> proc, long timeout, TimeUnit unit) throws SuspendExecution {
        mailbox.receive(wrapProcessor(proc), timeout, unit);
    }

    protected void receive(MessageProcessor<Message> proc) throws SuspendExecution {
        mailbox.receive(wrapProcessor(proc));
    }

    public void send(Message message) {
        mailbox.send(message);
    }

    public void sendSync(Message message) {
        mailbox.sendSync(message);
    }
    //</editor-fold>

    //<editor-fold desc="Lifecycle">
    /////////// Lifecycle ///////////////////////////////////
    @Override
    protected abstract void run() throws SuspendExecution;

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

    public void link(Actor other) {
        lifecycleListeners.add(other.lifecycleListener);
        other.lifecycleListeners.add(lifecycleListener);
    }

    public void unlink(Actor other) {
        lifecycleListeners.remove(other.lifecycleListener);
        other.lifecycleListeners.remove(lifecycleListener);
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
