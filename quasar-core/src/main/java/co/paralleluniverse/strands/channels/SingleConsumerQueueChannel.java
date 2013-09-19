/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.common.util.Objects;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class SingleConsumerQueueChannel<Message> extends QueueChannel<Message> implements Stranded {
    private Strand owner;

    public SingleConsumerQueueChannel(SingleConsumerQueue<Message, ?> queue, OverflowPolicy policy) {
        super(queue, policy, true);
    }

    public Object getOwner() {
        return owner;
    }

    public boolean isOwnerAlive() {
        return owner.isAlive();
    }

    @Override
    public void setStrand(Strand strand) {
        if (owner != null && strand != owner)
            throw new IllegalStateException("Channel " + this + " is already owned by " + owner);
        this.owner = strand;
    }

    @Override
    public Strand getStrand() {
        return owner;
    }

    protected void maybeSetCurrentStrandAsOwner() {
        if (owner == null)
            setStrand(Strand.currentStrand());
        else {
            assert Strand.equals(owner, Strand.currentStrand()) : "This method has been called by a different strand (thread or fiber) from that owning this object";
        }
    }

    Object tryReceiveNode() {
        return queue().pk();
    }

    Object receiveNode() throws SuspendExecution, InterruptedException {
        maybeSetCurrentStrandAsOwner();
        Object n;
        sync.register();
        for (int i = 0; (n = queue().pk()) == null; i++) {
            if (isSendClosed()) {
                setReceiveClosed();
                throw new EOFException();
            }
            sync.await(i);
        }
        sync.unregister();

        return n;
    }

    Object receiveNode(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (unit == null)
            return receiveNode();
        if (timeout <= 0)
            return tryReceiveNode();

        maybeSetCurrentStrandAsOwner();
        Object n;

        long left = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + left;
        
        sync.register();
        try {
            for (int i = 0; (n = queue().pk()) == null; i++) {
                if (isSendClosed()) {
                    setReceiveClosed();
                    throw new EOFException();
                }
                sync.await(i, left, TimeUnit.NANOSECONDS);

                left = deadline - System.nanoTime();
                if (left <= 0)
                    return null;
            }
        } finally {
            sync.unregister();
        }
        return n;
    }

    @Override
    public Message tryReceive() {
        if (isClosed())
            return null;
        final Object n = tryReceiveNode();
        if (n == null)
            return null;
        final Message m = queue().value(n);
        queue().deq(n);
        signalSenders();
        return m;
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        if (isClosed())
            return null;
        try {
            final Object n = receiveNode();
            final Message m = queue().value(n);
            queue().deq(n);
            signalSenders();
            return m;
        } catch (EOFException e) {
            return null;
        }
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (isClosed())
            return null;
        try {
            final Object n = receiveNode(timeout, unit);
            if (n == null)
                return null; // timeout
            final Message m = queue().value(n);
            queue().deq(n);
            signalSenders();
            return m;
        } catch (EOFException e) {
            return null;
        }
    }

    protected SingleConsumerQueue<Message, Object> queue() {
        return (SingleConsumerQueue<Message, Object>) queue;
    }

    @Override
    public String toString() {
        return "Channel{" + "owner: " + owner + ", sync: " + sync + ", queue: " + Objects.systemToString(queue) + '}';
    }
}
