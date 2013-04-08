/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.queues.SingleConsumerArrayLongQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerLinkedLongQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerLongQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class LongChannel extends Channel<Long> {
    public static LongChannel create(Thread owner, int mailboxSize) {
        return new LongChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayLongQueue(mailboxSize) : new SingleConsumerLinkedLongQueue());
    }

    public static LongChannel create(LightweightThread owner, int mailboxSize) {
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
