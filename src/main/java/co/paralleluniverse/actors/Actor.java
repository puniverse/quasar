/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public abstract class Actor<Message> extends LightweightThread {
    private final Mailbox<Message, ?> mailbox;
    
    private Actor(String name, ForkJoinPool fjPool, int stackSize, int mailboxSize) {
        super(name, fjPool, stackSize);
        this.mailbox = Mailbox.createMailbox(this, mailboxSize);
    }
    
    private Actor(String name, ForkJoinPool fjPool, int mailboxSize) {
        super(name, fjPool);
        this.mailbox = Mailbox.createMailbox(this, mailboxSize);
    }
    
    private Actor(ForkJoinPool fjPool, int stackSize, int mailboxSize) {
        this(null, fjPool, stackSize, mailboxSize);
    }
    
    private Actor(ForkJoinPool fjPool, int mailboxSize) {
        this(null, fjPool, mailboxSize);
    }
    
    private Actor(String name, int stackSize, int mailboxSize) {
        super(name, stackSize);
        this.mailbox = Mailbox.createMailbox(this, mailboxSize);
    }
    
    private Actor(String name, int mailboxSize) {
        super(name);
        this.mailbox = Mailbox.createMailbox(this, mailboxSize);
    }
    
    private Actor(int stackSize, int mailboxSize) {
        this((String)null, stackSize, mailboxSize);
    }
    
    private Actor(int mailboxSize) {
        this((String)null, mailboxSize);
    }

    protected Message receive() throws SuspendExecution {
        return mailbox.receive();
    }

    protected void receive(MessageProcessor<Message> proc, long timeout, TimeUnit unit, Message currentMessage) throws SuspendExecution {
        mailbox.receive(proc, timeout, unit, currentMessage);
    }

    protected void receive(MessageProcessor<Message> proc, Message currentMessage) throws SuspendExecution {
        mailbox.receive(proc, currentMessage);
    }

    protected void receive(MessageProcessor<Message> proc, long timeout, TimeUnit unit) throws SuspendExecution {
        mailbox.receive(proc, timeout, unit);
    }

    protected void receive(MessageProcessor<Message> proc) throws SuspendExecution {
        mailbox.receive(proc);
    }

    public void send(Message message) {
        mailbox.send(message);
    }

    public void sendSync(Message message) {
        mailbox.sendSync(message);
    }

    @Override
    protected abstract void run() throws SuspendExecution;
}
