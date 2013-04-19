/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.strands.queues.SingleConsumerArrayObjectQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedObjectQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class ObjectChannel<Message> extends Channel<Message> {
    public static <Message> ObjectChannel<Message> create(Object owner, int mailboxSize) {
        return new ObjectChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayObjectQueue<Message>(mailboxSize) : new SingleConsumerLinkedObjectQueue<Message>());
    }

    public static <Message> ObjectChannel<Message> create(int mailboxSize) {
        return new ObjectChannel(mailboxSize > 0 ? new SingleConsumerArrayObjectQueue<Message>(mailboxSize) : new SingleConsumerLinkedObjectQueue<Message>());
    }

    private ObjectChannel(Object owner, SingleConsumerQueue<Message, ?> queue) {
        super(owner, queue);
    }

    private ObjectChannel(SingleConsumerQueue<Message, ?> queue) {
        super(queue);
    }
}
