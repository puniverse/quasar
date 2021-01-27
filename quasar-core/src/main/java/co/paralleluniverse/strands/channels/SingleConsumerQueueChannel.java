/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.fibers.suspend.SuspendExecution;
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

    public SingleConsumerQueueChannel(SingleConsumerQueue<Message> queue, OverflowPolicy policy) {
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
        else
            assert Strand.equals(owner, Strand.currentStrand()) : "This method has been called by a different strand (" + Strand.currentStrand() + ") from that owning this object (" + owner + ")";
    }

    protected void checkClosed() throws EOFException {
        if (isClosed()) {
            if (getCloseException() != null)
                throw new ProducerException(getCloseException());
            throw EOFException.instance;
        }
    }

    @Override
    public Message tryReceive() {
        if (isClosed())
            return null;
        final Message m = queue().poll();
        if (m != null)
            signalSenders();
        return m;
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        if (isClosed())
            return null;
        try {
            maybeSetCurrentStrandAsOwner();
            Message m;
            Object token = sync.register();
            try {
                for (int i = 0; (m = queue().poll()) == null; i++) {
                    if (isSendClosed()) {
                        setReceiveClosed();
                        checkClosed();
                    }
                    sync.await(i);
                }
            } finally {
                sync.unregister(token);
            }

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
        if (unit == null)
            return receive();
        if (timeout <= 0)
            return tryReceive();
        try {
            maybeSetCurrentStrandAsOwner();
            Message m;

            long left = unit.toNanos(timeout);
            final long deadline = System.nanoTime() + left;

            Object token = sync.register();
            try {
                for (int i = 0; (m = queue().poll()) == null; i++) {
                    if (isSendClosed()) {
                        setReceiveClosed();
                        checkClosed();
                    }
                    sync.await(i, left, TimeUnit.NANOSECONDS);

                    left = deadline - System.nanoTime();
                    if (left <= 0)
                        break;
                }
            } finally {
                sync.unregister(token);
            }

            if (m != null)
                signalSenders();
            return m;
        } catch (EOFException e) {
            return null;
        }
    }

    protected SingleConsumerQueue<Message> queue() {
        return (SingleConsumerQueue<Message>) queue;
    }

    @Override
    public String toString() {
        return "Channel{" + "owner: " + owner + ", sync: " + sync + ", queue: " + Objects.systemToString(queue) + ", capacity: " + capacity() + '}';
    }
}
