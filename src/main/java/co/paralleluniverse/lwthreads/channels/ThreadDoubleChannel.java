/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.datastruct.SingleConsumerArrayDoubleQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerDoubleQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerLinkedDoubleQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class ThreadDoubleChannel extends ThreadChannel<Double> {
    public static ThreadDoubleChannel create(Thread owner, int mailboxSize) {
        return new ThreadDoubleChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayDoubleQueue(mailboxSize) : new SingleConsumerLinkedDoubleQueue());
    }

    private ThreadDoubleChannel(Thread owner, SingleConsumerQueue<Double, ?> queue) {
        super(owner, queue);
    }

    public double receiveInt() throws InterruptedException {
        return ((SingleConsumerDoubleQueue<Object>)queue).doubleValue(receiveNode());
    }

    public void send(double message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwner();
        }
    }

    public void sendSync(double message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwnerAndTryToExecNow();
        }
    }
}
