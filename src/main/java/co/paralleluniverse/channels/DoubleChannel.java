/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.channels;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.queues.SingleConsumerArrayDoubleQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerDoubleQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerLinkedDoubleQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class DoubleChannel extends Channel<Double> {
    public static DoubleChannel create(Thread owner, int mailboxSize) {
        return new DoubleChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayDoubleQueue(mailboxSize) : new SingleConsumerLinkedDoubleQueue());
    }

    public static DoubleChannel create(Fiber owner, int mailboxSize) {
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
