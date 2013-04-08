/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.queues.SingleConsumerArrayFloatQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerFloatQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerLinkedFloatQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class FloatChannel extends Channel<Float> {
    public static FloatChannel create(Thread owner, int mailboxSize) {
        return new FloatChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayFloatQueue(mailboxSize) : new SingleConsumerLinkedFloatQueue());
    }

    public static FloatChannel create(LightweightThread owner, int mailboxSize) {
        return new FloatChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayFloatQueue(mailboxSize) : new SingleConsumerLinkedFloatQueue());
    }

    private FloatChannel(Object owner, SingleConsumerQueue<Float, ?> queue) {
        super(owner, queue);
    }
    
    public float receiveFloat() throws SuspendExecution, InterruptedException {
        final Object n = receiveNode();
        final float m = ((SingleConsumerFloatQueue<Object>)queue).floatValue(n);
        queue.deq(n);
        return m;
    }

    public void send(float message) {
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signal();
        }
    }

    public void sendSync(float message) {
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signalAndTryToExecNow();
        }
    }
}
