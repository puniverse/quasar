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

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.queues.BasicSingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 * Single consumer!
 *
 * @author pron
 */
public class QueuePrimitiveChannel<Message> extends QueueChannel<Message> implements Stranded {
    private Strand owner;

    public QueuePrimitiveChannel(BasicSingleConsumerQueue<Message> queue, OverflowPolicy policy) {
        super(queue, policy, true);
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

    protected void checkClosed() throws EOFException {
        if (isClosed()) {
            if(getCloseException() != null)
                throw new ProducerException(getCloseException());
            throw EOFException.instance;
        }
    }

    public boolean hasMessage() {
        return queue().hasNext();
    }
    
    boolean awaitItem() throws SuspendExecution, InterruptedException, EOFException {
        maybeSetCurrentStrandAsOwner();
        Object n;
        Object token = sync.register();
        for (int i = 0; !queue().hasNext(); i++) {
            if (isSendClosed()) {
                setReceiveClosed();
                checkClosed();
            }
            sync.await(i);
        }
        sync.unregister(token);
        return true;
    }

    boolean awaitItem(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, EOFException {
        if (unit == null)
            return awaitItem();
        if (timeout <= 0)
            return queue().hasNext();

        maybeSetCurrentStrandAsOwner();
        Object n;

        long left = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + left;

        Object token = sync.register();
        try {
            for (int i = 0; !queue().hasNext(); i++) {
                if (isSendClosed()) {
                    setReceiveClosed();
                    checkClosed();
                }
                sync.await(i, left, TimeUnit.NANOSECONDS);

                left = deadline - System.nanoTime();
                if (left <= 0)
                    return false;
            }
        } finally {
            sync.unregister(token);
        }
        return true;
    }

    protected BasicSingleConsumerQueue<Message> queue() {
        return (BasicSingleConsumerQueue<Message>) queue;
    }
}
