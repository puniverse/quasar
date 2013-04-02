/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;

/**
 *
 * @author pron
 */
class Mailbox<Message, Node> {
    static <Message, Node> Mailbox<Message, Node> createMailbox(LightweightThread owner, SingleConsumerQueue<Message, Node> queue) {
        return new Mailbox<Message, Node>(owner, queue);
    }
    
    private final LightweightThread owner;
    private final SingleConsumerQueue<Message, Node> queue;

    Mailbox(LightweightThread owner, SingleConsumerQueue<Message, Node> queue) {
        this.owner = owner;
        this.queue = queue;
    }

    public Message receive() throws SuspendExecution {
        Message message;
        while ((message = queue.poll()) == null)
            LightweightThread.park(queue);
        return message;
    }

    public Message receive(MessagePredicate<Message> pred) throws SuspendExecution {
        Node n = null;
        for (;;) {
            n = queue.succ(n);
            if (n != null) {
                final Message m = queue.value(n);
                if(pred.matches(m)) {
                    queue.del(n);
                    return m;
                }
            }
            LightweightThread.park(this);
        }
    }
    
    public void send(Message message) {
        queue.enq(message);
        owner.unpark();
    }
}
