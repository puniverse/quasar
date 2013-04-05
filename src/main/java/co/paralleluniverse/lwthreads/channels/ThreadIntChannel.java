/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.datastruct.SingleConsumerArrayIntQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerIntQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerLinkedIntQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class ThreadIntChannel extends ThreadChannel<Integer> {
    public static ThreadIntChannel create(Thread owner, int mailboxSize) {
        return new ThreadIntChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayIntQueue(mailboxSize) : new SingleConsumerLinkedIntQueue());
    }

    private ThreadIntChannel(Thread owner, SingleConsumerQueue<Integer, ?> queue) {
        super(owner, queue);
    }

    public int receiveInt() throws InterruptedException {
        return ((SingleConsumerIntQueue<Object>)queue()).intValue(receiveNode());
    }

    public void send(int message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwner();
        }
    }

    public void sendSync(int message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwnerAndTryToExecNow();
        }
    }
}
