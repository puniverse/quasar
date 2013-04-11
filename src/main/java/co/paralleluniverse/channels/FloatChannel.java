/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.channels;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.queues.SingleConsumerArrayFloatQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerFloatQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerLinkedFloatQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class FloatChannel extends Channel<Float> {
    public static FloatChannel create(Thread owner, int mailboxSize) {
        return new FloatChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayFloatQueue(mailboxSize) : new SingleConsumerLinkedFloatQueue());
    }

    public static FloatChannel create(Fiber owner, int mailboxSize) {
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

    public float receiveFloat(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        final Object n = receiveNode(timeout, unit);
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
