/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerArrayIntQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerIntQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerLinkedIntQueue;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class IntChannel extends Channel<Integer> {
    public static IntChannel create(LightweightThread owner, int mailboxSize) {
        return new IntChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayIntQueue(mailboxSize) : new SingleConsumerLinkedIntQueue());
    }

    private IntChannel(LightweightThread owner, SingleConsumerQueue<Integer, ?> queue) {
        super(owner, queue);
    }

    public int receiveInt() throws SuspendExecution {
        final SingleConsumerQueue<Integer, Object> queue = queue();
        Object n;
        while((n = queue.pk()) == null)
            LightweightThread.park(queue);
        return ((SingleConsumerIntQueue<Object>)queue).intValue(n);
    }

    public void send(int message) {
        final SingleConsumerIntQueue<Object> queue = (SingleConsumerIntQueue<Object>)queue();
        if (getOwner().isAlive()) {
            queue.enq(message);
            getOwner().unpark();
        }
    }

    public void sendSync(int message) {
        final SingleConsumerIntQueue<Object> queue = (SingleConsumerIntQueue<Object>)queue();
        if (getOwner().isAlive()) {
            queue.enq(message);
            if (!getOwner().exec(this))
                getOwner().unpark();
        }
    }
}
