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

import co.paralleluniverse.common.util.SuspendableSupplier;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import com.google.common.base.Supplier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author circlespainter
 */
class TakeReceivePort<M> extends TransformingReceivePort<M> {
    private final AtomicLong missing = new AtomicLong();

    private volatile boolean closed = false;

    public TakeReceivePort(final ReceivePort<M> target, long count) {
        super(target);

        this.missing.set(count <= 0 ? 0 : count);
        
        if (count <= 0)
            closed = true;
    }

    public long getMissing() {
        return missing.get();
    }

    @Override
    public M tryReceive() {
        return countingReceive(new Supplier<M>() {
            @Override
            public M get() {
                return TakeReceivePort.super.tryReceive();
            }
        });
    }

    @Override
    public M receive() throws SuspendExecution, InterruptedException {
        return suspendableCountingReceive(new SuspendableSupplier<M>() {
            @Override
            public M get() throws SuspendExecution, InterruptedException {
               return TakeReceivePort.super.receive();
            }
        });
    }

    @Override
    public M receive(final Timeout timeout) throws SuspendExecution, InterruptedException {
        return suspendableCountingReceive(new SuspendableSupplier<M>() {
            @Override
            public M get() throws SuspendExecution, InterruptedException {
               return TakeReceivePort.super.receive(timeout);
            }
        });
    }

    @Override
    public M receive(final long timeout, final TimeUnit unit) throws SuspendExecution, InterruptedException {
        return suspendableCountingReceive(new SuspendableSupplier<M>() {
            @Override
            public M get() throws SuspendExecution, InterruptedException {
               return TakeReceivePort.super.receive(timeout, unit);
            }
        });
    }

    private M suspendableCountingReceive(final SuspendableSupplier<M> op) throws SuspendExecution, InterruptedException {
        if (isClosed())
            return null;

        missing.decrementAndGet();
        final M ret = op.get();
        if (ret == null)
            missing.incrementAndGet();
        return ret;
    }

    private M countingReceive(final Supplier<M> op) {
        try {
            return suspendableCountingReceive(new SuspendableSupplier<M>() {
                @Override
                public M get() throws SuspendExecution, InterruptedException {
                    return op.get();
                }
            });
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
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
