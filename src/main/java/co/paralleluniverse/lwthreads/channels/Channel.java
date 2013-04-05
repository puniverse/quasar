/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public abstract class Channel<Message> {
    final SingleConsumerQueue<Message, Object> queue;

    Channel(SingleConsumerQueue<Message, ?> queue) {
        this.queue = (SingleConsumerQueue<Message, Object>) queue;
    }

    abstract boolean isOwnerAlive();

    abstract void notifyOwner();

    abstract void notifyOwnerAndTryToExecNow();

    public void send(Message message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwner();
        }
    }

    public void sendSync(Message message) {
        if (isOwnerAlive()) {
            queue.enq(message);
            notifyOwnerAndTryToExecNow();
        }
    }
}
