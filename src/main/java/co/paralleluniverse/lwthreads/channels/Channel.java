/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public abstract class Channel<Message> {
    private final LightweightThread owner;
    private final SingleConsumerQueue<Message, ?> queue;

    Channel(LightweightThread owner, SingleConsumerQueue<Message, ?> queue) {
        this.owner = owner;
        this.queue = queue;
    }

    <Node> SingleConsumerQueue<Message, Node> queue() {
        return (SingleConsumerQueue<Message, Node>)queue;
    }
    
    public LightweightThread getOwner() {
        return owner;
    }

    public Message receive() throws SuspendExecution {
        Message message;
        while ((message = queue.poll()) == null)
            LightweightThread.park(queue);
        return message;
    }

    public void send(Message message) {
        if (owner.isAlive()) {
            queue.enq(message);
            owner.unpark();
        }
    }

    public void sendSync(Message message) {
        if (owner.isAlive()) {
            queue.enq(message);
            if (!owner.exec(this))
                owner.unpark();
        }
    }
}
