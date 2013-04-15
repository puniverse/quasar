/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.OwnedSynchronizer;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public abstract class Channel<Message> implements SendChannel<Message>, Stranded {
    private Object owner;
    private OwnedSynchronizer sync;
    final SingleConsumerQueue<Message, Object> queue;

    Channel(Object owner, SingleConsumerQueue<Message, ?> queue) {
        this.queue = (SingleConsumerQueue<Message, Object>) queue;
        this.owner = owner;
        this.sync = OwnedSynchronizer.create(owner);
    }

    Channel(SingleConsumerQueue<Message, ?> queue) {
        this.queue = (SingleConsumerQueue<Message, Object>) queue;
    }

    public Object getOwner() {
        return owner;
    }

    @Override
    public void setStrand(Strand strand) {
        this.owner = strand;
        this.sync = OwnedSynchronizer.create(owner);
    }

    protected void maybeSetCurrentStrandAsOwner() {
        if (owner == null)
            setStrand(Strand.currentStrand());
        else
            sync.verifyOwner();
    }

    protected OwnedSynchronizer sync() {
        verifySync();
        return sync;
    }

    @Override
    public Strand getStrand() {
        return (Strand) owner;
    }

    @Override
    public void send(Message message) {
        verifySync();
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signal();
        }
    }

    public void sendSync(Message message) {
        verifySync();
        if (sync.isOwnerAlive()) {
            queue.enq(message);
            sync.signalAndTryToExecNow();
        }
    }

    Object receiveNode() throws SuspendExecution, InterruptedException {
        maybeSetCurrentStrandAsOwner();
        Object n;
        sync.lock();
        while ((n = queue.pk()) == null)
            sync.await();
        sync.unlock();

        return n;
    }

    Object receiveNode(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (timeout <= 0 || unit == null)
            return receiveNode();

        maybeSetCurrentStrandAsOwner();
        Object n;

        final long start = System.nanoTime();
        long left = unit.toNanos(timeout);

        sync.lock();
        try {
            while ((n = queue.pk()) == null) {
                sync.await(this, left, TimeUnit.NANOSECONDS);

                left = start + unit.toNanos(timeout) - System.nanoTime();
                if (left <= 0)
                    return null;
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
        if (n == null)
            return null; // timeout
        final Message m = queue.value(n);
        queue.deq(n);
        return m;
    }

    private void verifySync() {
        if (sync == null)
            throw new IllegalStateException("Owning strand has not been set");
    }
}
