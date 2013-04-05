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
public class LwtLongChannel extends LwtChannel<Long> {
    public static LwtLongChannel create(LightweightThread owner, int mailboxSize) {
        return new LwtLongChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayLongQueue(mailboxSize) : new SingleConsumerLinkedLongQueue());
    }

    private LwtLongChannel(LightweightThread owner, SingleConsumerQueue<Long, ?> queue) {
        super(owner, queue);
    }

    public long receiveInt() throws SuspendExecution {
        return ((SingleConsumerLongQueue<Object>)queue()).longValue(receiveNode());
    }

    public void send(long message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwner();
        }
    }

    public void sendSync(long message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwnerAndTryToExecNow();
        }
    }
}
