/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.queues.SingleConsumerArrayLongQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedLongQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLongQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class LongChannel extends Channel<Long> {
    public static LongChannel create(Object owner, int mailboxSize) {
        return new LongChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayLongQueue(mailboxSize) : new SingleConsumerLinkedLongQueue());
    }

    public static LongChannel create(int mailboxSize) {
        return new LongChannel(mailboxSize > 0 ? new SingleConsumerArrayLongQueue(mailboxSize) : new SingleConsumerLinkedLongQueue());
    }

    private LongChannel(Object owner, SingleConsumerQueue<Long, ?> queue) {
        super(owner, queue);
    }

    private LongChannel(SingleConsumerQueue<Long, ?> queue) {
        super(queue);
    }

    public long receiveLong() throws SuspendExecution, InterruptedException {
        final Object n = receiveNode();
        final long m = ((SingleConsumerLongQueue<Object>) queue).longValue(n);
        queue.deq(n);
        return m;
    }

    public long receiveLong(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        final Object n = receiveNode(timeout, unit);
        final long m = ((SingleConsumerLongQueue<Object>) queue).longValue(n);
        queue.deq(n);
        return m;
    }

    public void send(long message) {
        queue.enq(message);
        signal();
    }

    public void sendSync(long message) {
        queue.enq(message);
        signalAndTryToExecNow();
    }
}
