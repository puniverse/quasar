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
 * A channel owned by a LightweightThread
 *
 * @author pron
 */
abstract class LwtChannel<Message> extends Channel<Message> {
    private final LightweightThread owner;

    public LwtChannel(LightweightThread owner, SingleConsumerQueue<Message, ?> queue) {
        super(queue);
        this.owner = owner;
    }

    public LightweightThread getOwner() {
        return owner;
    }

    @Override
    boolean isOwnerAlive() {
        return owner.isAlive();
    }

    @Override
    void notifyOwner() {
        owner.unpark();
    }

    @Override
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
}
