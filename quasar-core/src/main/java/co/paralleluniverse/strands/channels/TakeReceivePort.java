/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.strands.Condition;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.Timeout;
import com.google.common.base.Supplier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author pron
 * @author circlespainter
 */
class TakeReceivePort<M> extends TransformingReceivePort<M> {
    private final AtomicLong lease = new AtomicLong();
    private final AtomicLong countDown = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Condition monitor = new SimpleConditionSynchronizer(null);

    public TakeReceivePort(final ReceivePort<M> target, final long count) {
        super(target);

        final long start = count <= 0 ? 0 : count;
        this.lease.set(start);
        this.countDown.set(start);

        this.closed.set(false);
    }

    @Override
    public M tryReceive() {
        return stagedCountingReceive(new Supplier<M>() {
            @Override
            public M get() {
                return TakeReceivePort.super.tryReceive();
            }
        });
    }

    @Override
    public M receive() throws SuspendExecution, InterruptedException {
        return suspendableStagedCountingReceive(new SuspendableSupplier<M>() {
            @Override
            public M get() throws SuspendExecution, InterruptedException {
               return TakeReceivePort.super.receive();
            }
        });
    }

    @Override
    public M receive(final Timeout timeout) throws SuspendExecution, InterruptedException {
        return suspendableStagedCountingReceive(new SuspendableSupplier<M>() {
            @Override
            public M get() throws SuspendExecution, InterruptedException {
               return TakeReceivePort.super.receive(timeout);
            }
        });
    }

    @Override
    public M receive(final long timeout, final TimeUnit unit) throws SuspendExecution, InterruptedException {
        return suspendableStagedCountingReceive(new SuspendableSupplier<M>() {
            @Override
            public M get() throws SuspendExecution, InterruptedException {
               return TakeReceivePort.super.receive(timeout, unit);
            }
        });
    }

    private M suspendableStagedCountingReceive(final SuspendableSupplier<M> op) throws SuspendExecution, InterruptedException {
        M ret = null;
        final Object ticket = monitor.register();
        try {
            int iter = 0;
            while (ret == null) {
                if (isClosed()) {
                    monitor.signalAll();
                    return null;
                }

                if (lease.decrementAndGet() <= 0)
                    monitor.await(iter);
                else {
                    ret = op.get();
                    if (ret != null) {
                        countDown.decrementAndGet();
                        return ret;
                    } else {
                        lease.incrementAndGet();
                        monitor.signal();
                    }
                }
                iter++;
            }
        } finally {
            monitor.unregister(ticket);
        }

        return ret;
    }

    private M stagedCountingReceive(final Supplier<M> op) {
        try {
            return suspendableStagedCountingReceive(new SuspendableSupplier<M>() {
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
        return closed.get() || countDown.get() <= 0;
    }

    @Override
    public void close() {
        closed.set(true);
    }
}
