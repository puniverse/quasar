/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerArrayFloatQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerFloatQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerLinkedFloatQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class LwtFloatChannel extends LwtChannel<Float> {
    public static LwtFloatChannel create(LightweightThread owner, int mailboxSize) {
        return new LwtFloatChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayFloatQueue(mailboxSize) : new SingleConsumerLinkedFloatQueue());
    }

    private LwtFloatChannel(LightweightThread owner, SingleConsumerQueue<Float, ?> queue) {
        super(owner, queue);
    }

    public float receiveFloat() throws SuspendExecution {
        return ((SingleConsumerFloatQueue<Object>)queue).floatValue(receiveNode());
    }

    public void send(float message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwner();
        }
    }

    public void sendSync(float message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwnerAndTryToExecNow();
        }
    }
}
