/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.TimeoutException;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Mailbox;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public abstract class BasicActor<Message, V> extends Actor<Message, V> {
    private Message currentMessage; // this works because channel is single-consumer

    public BasicActor(String name, int mailboxSize) {
        super(name, mailboxSize);
    }

    public BasicActor(int mailboxSize) {
        this((String) null, mailboxSize);
    }

    public BasicActor() {
        this((String) null, -1);
    }

    public BasicActor(Strand strand, String name, int mailboxSize) {
        super(strand, name, mailboxSize);
    }

    public BasicActor(Strand strand, int mailboxSize) {
        this(strand, (String) null, mailboxSize);
    }

    /**
     *
     * @param proc
     * @param currentMessage
     * @param timeout
     * @param unit
     * @throws TimeoutException
     * @throws LwtInterruptedException
     */
    public Message receive(long timeout, TimeUnit unit, MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        checkThrownIn();
        final Mailbox<Object> mailbox = mailbox();
        mailbox.maybeSetCurrentStrandAsOwner();

        final long start = timeout > 0 ? System.nanoTime() : 0;
        long now;
        long left = unit != null ? unit.toNanos(timeout) : 0;

        Object n = null;
        for (;;) {
            if (flightRecorder != null)
                record(1, "Actor", "receive", "%s waiting for a message. %s", this, timeout > 0 ? "millis left: " + TimeUnit.MILLISECONDS.convert(left, TimeUnit.NANOSECONDS) : "");

            mailbox.lock();
            n = mailbox.succ(n);

            if (n != null) {
                mailbox.unlock();
                final Object m = mailbox.value(n);
                if (m == currentMessage) {
                    mailbox.del(n);
                    continue;
                }

                record(1, "Actor", "receive", "Received %s <- %s", this, m);
                try {
                    if (m instanceof LifecycleMessage) {
                        handleLifecycleMessage((LifecycleMessage) m);
                        mailbox.del(n);
                    } else {
                        final Message msg = (Message) m;
                        currentMessage = msg;
                        if (proc.process(msg)) {
                            if (mailbox.value(n) == msg) // another call to receive from within the processor may have deleted n
                                mailbox.del(n);
                            return msg;
                        }
                    }

                } catch (Exception e) {
                    if (mailbox.value(n) == m) // another call to receive from within the processor may have deleted n
                        mailbox.del(n);
                    throw e;
                }
            } else {
                try {
                    if (timeout > 0) {
                        mailbox.await(left, TimeUnit.NANOSECONDS);

                        now = System.nanoTime();
                        left = start + unit.toNanos(timeout) - now;
                        if (left <= 0) {
                            record(1, "Actor", "receive", "%s timed out.", this);
                            throw new TimeoutException();
                        }
                    } else
                        mailbox.await();
                } finally {
                    mailbox.unlock();
                }
            }
        }
    }

    public Message receive(MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        return receive(0, null, proc);
    }
}
