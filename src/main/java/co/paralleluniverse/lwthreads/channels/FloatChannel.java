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
public class FloatChannel extends Channel<Float> {
    public static FloatChannel create(LightweightThread owner, int mailboxSize) {
        return new FloatChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayFloatQueue(mailboxSize) : new SingleConsumerLinkedFloatQueue());
    }

    private FloatChannel(LightweightThread owner, SingleConsumerQueue<Float, ?> queue) {
        super(owner, queue);
    }

    public float receiveFloat() throws SuspendExecution {
        final SingleConsumerQueue<Float, Object> queue = queue();
        Object n;
        while((n = queue.pk()) == null)
            LightweightThread.park(queue);
        return ((SingleConsumerFloatQueue<Object>)queue).floatValue(n);
    }

    public void send(float message) {
        final SingleConsumerFloatQueue<Object> queue = (SingleConsumerFloatQueue<Object>)queue();
        if (getOwner().isAlive()) {
            queue.enq(message);
            getOwner().unpark();
        }
    }

    public void sendSync(float message) {
        final SingleConsumerFloatQueue<Object> queue = (SingleConsumerFloatQueue<Object>)queue();
        if (getOwner().isAlive()) {
            queue.enq(message);
            if (!getOwner().exec(this))
                getOwner().unpark();
        }
    }
}
