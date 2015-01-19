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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author circlespainter
 */
class TakeReceivePort<M> extends TransformingReceivePort<M> {
    private final ReceivePort<M> realTarget;
    private final Channel<M> internalCh;

    private final AtomicLong missing = new AtomicLong();
    private final long count;

    private volatile boolean closed = false;

    public TakeReceivePort(final ReceivePort<M> target, long count) {
        super(new TransferChannel<M>());

        this.realTarget = target;
        this.internalCh = (Channel<M>) this.target;

        this.count = count;
        this.missing.set(count <= 0 ? 0 : count);
        
        if (count <= 0)
            closed = true;
    }

    public long getMissing() {
        return missing.get();
    }

    @Override
    public M tryReceive() {
        if (isClosed())
            return null;

        missing.decrementAndGet();
        final M ret = super.tryReceive();
        if (ret == null)
            missing.incrementAndGet();
        return ret;
    }

    @Override
    public M receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        if (isClosed())
            return null;

        missing.decrementAndGet();
        return super.receive(timeout);
    }

    @Override
    public M receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (isClosed())
            return null;

        missing.decrementAndGet();
        return super.receive(timeout, unit);
    }

    @Override
    public boolean isClosed() {
        return closed || missing.get() == 0;
    }

    @Override
    public void close() {
        this.closed = true;
    }
}
