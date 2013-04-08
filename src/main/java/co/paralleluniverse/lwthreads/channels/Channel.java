/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.channels;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.queues.SingleConsumerQueue;
import co.paralleluniverse.lwthreads.sync.OwnedSynchronizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author pron
 */
public abstract class Channel<Message> implements SendChannel<Message> {
    private final Object owner;
    final OwnedSynchronizer sync;
    private final Lock lock = new ReentrantLock();
    private final Condition queueNotEmpty = lock.newCondition();
    final SingleConsumerQueue<Message, Object> queue;

    Channel(Object owner, SingleConsumerQueue<Message, ?> queue) {
        if (!(owner instanceof LightweightThread || owner instanceof Thread))
            throw new IllegalArgumentException("owner must be a Thread or a LightweightThread but is " + owner.getClass().getName());
        this.queue = (SingleConsumerQueue<Message, Object>) queue;
        this.owner = owner;
        this.sync = OwnedSynchronizer.create(owner);
    }

    public Object getOwner() {
        return owner;
    }

    @Override
    public void send(Message message) {
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signal();
        }
    }

    public void sendSync(Message message) {
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signalAndTryToExecNow();
        }
    }

    Object receiveNode() throws SuspendExecution, InterruptedException {
        sync.verifyOwner();
        Object n;
        sync.lock();
        try {
            while ((n = queue.pk()) == null)
                sync.await();
        } finally {
            sync.unlock();
        }
        return n;
    }

    Object receiveNode(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        sync.verifyOwner();
        Object n;

        final long start = timeout > 0 ? System.nanoTime() : 0;
        long left = unit != null ? unit.toNanos(timeout) : 0;
        
        sync.lock();
        try {
            while ((n = queue.pk()) == null) {
                if (timeout > 0) {
                    sync.await(this, left, TimeUnit.NANOSECONDS);

                    left = start + unit.toNanos(timeout) - System.nanoTime();
                    if (left <= 0)
                        return null;
                } else
                    sync.await();
            }
        } finally {
            sync.unlock();
        }
        return n;
    }

    public Message receive() throws SuspendExecution, InterruptedException {
        final Object n = receiveNode();
        final Message m = queue.value(n);
        queue.deq(n);
        return m;
    }

    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        final Object n = receiveNode(timeout, unit);
        final Message m = queue.value(n);
        queue.deq(n);
        return m;
    }
}
