/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.queues.SingleConsumerArrayDoubleQueue;
import co.paralleluniverse.strands.queues.SingleConsumerDoubleQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedDoubleQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class DoubleChannel extends Channel<Double> {
    public static DoubleChannel create(int mailboxSize) {
        return new DoubleChannel(mailboxSize > 0 ? new SingleConsumerArrayDoubleQueue(mailboxSize) : new SingleConsumerLinkedDoubleQueue());
    }

    public static DoubleChannel create(Object owner, int mailboxSize) {
        return new DoubleChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayDoubleQueue(mailboxSize) : new SingleConsumerLinkedDoubleQueue());
    }

    private DoubleChannel(Object owner, SingleConsumerQueue<Double, ?> queue) {
        super(owner, queue);
    }

    private DoubleChannel(SingleConsumerQueue<Double, ?> queue) {
        super(queue);
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
        queue.enq(message);
        signal();
    }

    public void sendSync(double message) {
        queue.enq(message);
        signalAndTryToExecNow();
    }
}
