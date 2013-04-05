/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.datastruct.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A channel owned by a LightweightThread
 *
 * @author pron
 */
abstract class ThreadChannel<Message> extends Channel<Message> {
    private final Thread owner;
    final Lock lock = new ReentrantLock();
    private final Condition queueNotEmpty = lock.newCondition();

    public ThreadChannel(Thread owner, SingleConsumerQueue<Message, ?> queue) {
        super(queue);
        this.owner = owner;
    }

    public Thread getOwner() {
        return owner;
    }

    @Override
    boolean isOwnerAlive() {
        return owner.isAlive();
    }

    @Override
    void notifyOwner() {
        queueNotEmpty.signal();
    }

    @Override
    void notifyOwnerAndTryToExecNow() {
        notifyOwner();
    }

    void await() throws InterruptedException {
        queueNotEmpty.await();
    }

    void await(Object blocker, long timeout, TimeUnit unit) throws InterruptedException {
        queueNotEmpty.await(timeout, unit);
    }

    Object receiveNode() throws InterruptedException {
        assert Thread.currentThread() == owner;
        Object n;
        lock.lock();
        try {
            while ((n = queue.pk()) == null)
                await();
        } finally {
            lock.unlock();
        }
        return n;
    }

    public Message receive() throws InterruptedException {
        assert Thread.currentThread() == owner;
        return queue.value(receiveNode());
    }
}
