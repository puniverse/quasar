/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.queues.SingleConsumerArrayDoubleQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerDoubleQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerLinkedDoubleQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class DoubleChannel extends Channel<Double> {
    public static DoubleChannel create(Thread owner, int mailboxSize) {
        return new DoubleChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayDoubleQueue(mailboxSize) : new SingleConsumerLinkedDoubleQueue());
    }

    public static DoubleChannel create(LightweightThread owner, int mailboxSize) {
        return new DoubleChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayDoubleQueue(mailboxSize) : new SingleConsumerLinkedDoubleQueue());
    }

    private DoubleChannel(Object owner, SingleConsumerQueue<Double, ?> queue) {
        super(owner, queue);
    }

    public double receiveDouble() throws SuspendExecution, InterruptedException {
        final Object n = receiveNode();
        final double m = ((SingleConsumerDoubleQueue<Object>) queue).doubleValue(n);
        queue.deq(n);
        return m;
    }

    public double receiveDouble(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        final Object n = receiveNode(timeout, unit);
        final double m = ((SingleConsumerDoubleQueue<Object>) queue).doubleValue(n);
        queue.deq(n);
        return m;
    }

    public void send(double message) {
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signal();
        }
    }

    public void sendSync(double message) {
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signalAndTryToExecNow();
        }
    }
}
