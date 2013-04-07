/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.queues.SingleConsumerArrayIntQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerIntQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerLinkedIntQueue;
import co.paralleluniverse.lwthreads.queues.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class IntChannel extends Channel<Integer> {
    public static IntChannel create(Thread owner, int mailboxSize) {
        return new IntChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayIntQueue(mailboxSize) : new SingleConsumerLinkedIntQueue());
    }

    public static IntChannel create(LightweightThread owner, int mailboxSize) {
        return new IntChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayIntQueue(mailboxSize) : new SingleConsumerLinkedIntQueue());
    }

    private IntChannel(Object owner, SingleConsumerQueue<Integer, ?> queue) {
        super(owner, queue);
    }

    public int receiveInt() throws SuspendExecution, InterruptedException {
        return ((SingleConsumerIntQueue<Object>)queue).intValue(receiveNode());
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
