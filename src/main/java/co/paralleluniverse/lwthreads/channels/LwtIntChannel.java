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
public class LwtIntChannel extends LwtChannel<Integer> {
    public static LwtIntChannel create(LightweightThread owner, int mailboxSize) {
        return new LwtIntChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayIntQueue(mailboxSize) : new SingleConsumerLinkedIntQueue());
    }

    private LwtIntChannel(LightweightThread owner, SingleConsumerQueue<Integer, ?> queue) {
        super(owner, queue);
    }

    public int receiveInt() throws SuspendExecution {
        return ((SingleConsumerIntQueue<Object>)queue()).intValue(receiveNode());
    }

    public void send(int message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwner();
        }
    }

    public void sendSync(int message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwnerAndTryToExecNow();
        }
    }
}
