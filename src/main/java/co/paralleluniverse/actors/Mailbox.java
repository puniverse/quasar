/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.LwtInterruptedException;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.TimeoutException;
import co.paralleluniverse.lwthreads.datastruct.QueueCapacityExceededException;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerArrayQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerLinkedQueue1;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
class Mailbox<Message, Node> {
    public static <Message, Node> Mailbox<Message, Node> createMailbox(LightweightThread owner, SingleConsumerQueue<Message, Node> queue) {
        return new Mailbox<Message, Node>(owner, queue);
    }

    public static <Message> Mailbox<Message, ?> createMailbox(LightweightThread owner, int mailboxSize) {
        return new Mailbox(owner, mailboxSize > 0 ? new SingleConsumerArrayQueue<Message>(mailboxSize) : new SingleConsumerLinkedQueue1<Message>());
    }
    private final LightweightThread owner;
    private final SingleConsumerQueue<Message, Node> queue;

    private Mailbox(LightweightThread owner, SingleConsumerQueue<Message, Node> queue) {
        this.owner = owner;
        this.queue = queue;
    }

    public Message receive() throws SuspendExecution {
        Message message;
        while ((message = queue.poll()) == null)
            LightweightThread.park(queue);
        return message;
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
    public void receive(MessageProcessor<Message> proc, long timeout, TimeUnit unit, Message currentMessage) throws SuspendExecution {
        final long start = timeout > 0 ? System.nanoTime() : 0;
        long now;
        long left = unit != null ? unit.toNanos(timeout) : 0;

        Node n = null;
        for (;;) {
            n = queue.succ(n);
            if (n != null) {
                final Object m = queue.value(n);
                if (m == currentMessage) {
                    queue.del(n);
                    continue;
                }

                try {
                    if (proc.process((Message) m)) {
                        if (queue.value(n) == m) // another call to receive from within the processor may have deleted n
                            queue.del(n);
                        break;
                    }
                } catch (Exception e) {
                    if (queue.value(n) == m) // another call to receive from within the processor may have deleted n
                        queue.del(n);
                    throw e;
                }
            }

            if (timeout > 0) {
                LightweightThread.park(this, left, TimeUnit.NANOSECONDS);

                now = System.nanoTime();
                left = start + unit.toNanos(timeout) - now;
                if (left <= 0)
                    throw new TimeoutException();
            } else
                LightweightThread.park(this);
        }
    }

    public void receive(MessageProcessor<Message> proc, Message currentMessage) throws SuspendExecution {
        receive(proc, 0, null, currentMessage);
    }

    public void receive(MessageProcessor<Message> proc, long timeout, TimeUnit unit) throws SuspendExecution {
        receive(proc, timeout, unit, null);
    }

    public void receive(MessageProcessor<Message> proc) throws SuspendExecution {
        receive(proc, 0, null, null);
    }

    public void send(Message message) {
        if (owner.isAlive()) {
            enq(message);
            owner.unpark();
        }
    }

    public void sendSync(Message message) {
        if (owner.isAlive()) {
            enq(message);
            if (!owner.exec(this))
                owner.unpark();
        }
    }

    private void enq(Message message) {
        try {
            queue.enq(message);
        } catch (QueueCapacityExceededException e) {
            owner.interrupt();
        }
    }
}
