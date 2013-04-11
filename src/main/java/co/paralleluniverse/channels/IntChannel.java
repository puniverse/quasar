/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.channels;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.queues.SingleConsumerArrayIntQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerIntQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerLinkedIntQueue;
import co.paralleluniverse.fibers.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class IntChannel extends Channel<Integer> {
    public static IntChannel create(Thread owner, int mailboxSize) {
        return new IntChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayIntQueue(mailboxSize) : new SingleConsumerLinkedIntQueue());
    }

    public static IntChannel create(Fiber owner, int mailboxSize) {
        return new IntChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayIntQueue(mailboxSize) : new SingleConsumerLinkedIntQueue());
    }

    private IntChannel(Object owner, SingleConsumerQueue<Integer, ?> queue) {
        super(owner, queue);
    }

    public int receiveInt() throws SuspendExecution, InterruptedException {
        final Object n = receiveNode();
        final int m = ((SingleConsumerIntQueue<Object>)queue).intValue(n);
        queue.deq(n);
        return m;
    }

    public int receiveInt(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        final Object n = receiveNode(timeout, unit);
        final int m = ((SingleConsumerIntQueue<Object>)queue).intValue(n);
        queue.deq(n);
        return m;
    }

    public void send(int message) {
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signal();
        }
    }

    public void sendSync(int message) {
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signalAndTryToExecNow();
        }
    }
}
