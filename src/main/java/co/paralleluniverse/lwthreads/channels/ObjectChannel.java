/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.actors.MessageProcessor;
import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.TimeoutException;
import co.paralleluniverse.lwthreads.queues.SingleConsumerArrayObjectQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerLinkedObjectQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class ObjectChannel<Message> extends Channel<Message> {
    public static <Message> ObjectChannel<Message> create(Thread owner, int mailboxSize) {
        return new ObjectChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayObjectQueue<Message>(mailboxSize) : new SingleConsumerLinkedObjectQueue<Message>());
    }

    public static <Message> ObjectChannel<Message> create(LightweightThread owner, int mailboxSize) {
        return new ObjectChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayObjectQueue<Message>(mailboxSize) : new SingleConsumerLinkedObjectQueue<Message>());
    }

    private ObjectChannel(Object owner, SingleConsumerQueue<Message, ?> queue) {
        super(owner, queue);
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
    public Message receive(MessageProcessor<Message> proc, long timeout, TimeUnit unit, Message currentMessage) throws SuspendExecution, InterruptedException {
        assert Thread.currentThread() == getOwner();

        final long start = timeout > 0 ? System.nanoTime() : 0;
        long now;
        long left = unit != null ? unit.toNanos(timeout) : 0;

        Object n = null;
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
                        return (Message) m;
                    }
                } catch (Exception e) {
                    if (queue.value(n) == m) // another call to receive from within the processor may have deleted n
                        queue.del(n);
                    throw e;
                }
            }

            sync.lock();
            try {
                if (timeout > 0) {
                    sync.await(this, left, TimeUnit.NANOSECONDS);

                    now = System.nanoTime();
                    left = start + unit.toNanos(timeout) - now;
                    if (left <= 0)
                        throw new TimeoutException();
                } else
                    sync.await();
            } finally {
                sync.unlock();
            }
        }
    }

    public Message receive(MessageProcessor<Message> proc, Message currentMessage) throws SuspendExecution, InterruptedException {
        return receive(proc, 0, null, currentMessage);
    }

    public Message receive(MessageProcessor<Message> proc, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return receive(proc, timeout, unit, null);
    }

    public Message receive(MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        return receive(proc, 0, null, null);
    }
}
