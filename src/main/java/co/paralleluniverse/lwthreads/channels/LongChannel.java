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
    public static LongChannel create(LightweightThread owner, int mailboxSize) {
        return new LongChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayLongQueue(mailboxSize) : new SingleConsumerLinkedLongQueue());
    }

    private LongChannel(LightweightThread owner, SingleConsumerQueue<Long, ?> queue) {
        super(owner, queue);
    }

    public long receiveInt() throws SuspendExecution {
        return ((SingleConsumerLongQueue<Object>)queue()).longValue(receiveNode());
    }

    public void send(long message) {
        final SingleConsumerLongQueue<Object> queue = (SingleConsumerLongQueue<Object>)queue();
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwner();
        }
    }

    public void sendSync(long message) {
        final SingleConsumerLongQueue<Object> queue = (SingleConsumerLongQueue<Object>)queue();
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwnerAndTryToExecNow();
        }
    }
}
