/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.channels;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.queues.SingleConsumerArrayLongQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerLinkedLongQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerLongQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class LongChannel extends Channel<Long> {
    public static LongChannel create(Thread owner, int mailboxSize) {
        return new LongChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayLongQueue(mailboxSize) : new SingleConsumerLinkedLongQueue());
    }

    public static LongChannel create(Fiber owner, int mailboxSize) {
        return new LongChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayLongQueue(mailboxSize) : new SingleConsumerLinkedLongQueue());
    }

    private LongChannel(Object owner, SingleConsumerQueue<Long, ?> queue) {
        super(owner, queue);
    }

    public long receiveLong() throws SuspendExecution, InterruptedException {
        final Object n = receiveNode();
        final long m = ((SingleConsumerLongQueue<Object>)queue).longValue(n);
        queue.deq(n);
        return m;
    }

    public long receiveLong(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        final Object n = receiveNode(timeout, unit);
        final long m = ((SingleConsumerLongQueue<Object>)queue).longValue(n);
        queue.deq(n);
        return m;
    }

    public void send(long message) {
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signal();
        }
    }

    public void sendSync(long message) {
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signalAndTryToExecNow();
        }
    }
}
