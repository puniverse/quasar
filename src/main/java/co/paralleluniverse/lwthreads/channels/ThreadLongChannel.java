/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.datastruct.SingleConsumerArrayLongQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerLinkedLongQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerLongQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class ThreadLongChannel extends ThreadChannel<Long> {
    public static ThreadLongChannel create(Thread owner, int mailboxSize) {
        return new ThreadLongChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayLongQueue(mailboxSize) : new SingleConsumerLinkedLongQueue());
    }

    private ThreadLongChannel(Thread owner, SingleConsumerQueue<Long, ?> queue) {
        super(owner, queue);
    }

    public long receiveInt() throws InterruptedException {
        return ((SingleConsumerLongQueue<Object>)queue).longValue(receiveNode());
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
