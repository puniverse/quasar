/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerArrayLongQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerLinkedLongQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerLongQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;

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

    public long receiveInt() throws SuspendExecution, InterruptedException {
        return ((SingleConsumerLongQueue<Object>)queue).longValue(receiveNode());
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
