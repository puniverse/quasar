/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public abstract class Channel<Message> {
    private final LightweightThread owner;
    private final SingleConsumerQueue<Message, Object> queue;

    Channel(LightweightThread owner, SingleConsumerQueue<Message, ?> queue) {
        this.owner = owner;
        this.queue = (SingleConsumerQueue<Message, Object>) queue;
    }

    <Node> SingleConsumerQueue<Message, Node> queue() {
        return (SingleConsumerQueue<Message, Node>) queue;
    }

    public LightweightThread getOwner() {
        return owner;
    }

    boolean isOwnerAlive() {
        return owner.isAlive();
    }

    void notifyOwner() {
        owner.unpark();
    }

    void notifyOwnerAndTryToExecNow() {
        if (!owner.exec(this))
            owner.unpark();
    }

    void await() throws SuspendExecution {
        LightweightThread.park(this);
    }

    void await(Object blocker, long timeout, TimeUnit unit) throws SuspendExecution {
        LightweightThread.park(this, timeout, unit);
    }

    Object receiveNode() throws SuspendExecution {
        assert LightweightThread.currentLightweightThread() == owner;
        Object n;
        while ((n = queue.pk()) == null)
            await();
        return n;
    }

    public Message receive() throws SuspendExecution {
        assert LightweightThread.currentLightweightThread() == owner;
        return queue.value(receiveNode());
    }

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
