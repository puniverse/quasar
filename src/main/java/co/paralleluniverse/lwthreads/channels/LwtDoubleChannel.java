/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerArrayDoubleQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerDoubleQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerLinkedDoubleQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class LwtDoubleChannel extends LwtChannel<Double> {
    public static LwtDoubleChannel create(LightweightThread owner, int mailboxSize) {
        return new LwtDoubleChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayDoubleQueue(mailboxSize) : new SingleConsumerLinkedDoubleQueue());
    }

    private LwtDoubleChannel(LightweightThread owner, SingleConsumerQueue<Double, ?> queue) {
        super(owner, queue);
    }

    public double receiveInt() throws SuspendExecution {
        return ((SingleConsumerDoubleQueue<Object>)queue()).doubleValue(receiveNode());
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
