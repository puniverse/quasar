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

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Condition;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import co.paralleluniverse.concurrent.util.EnhancedAtomicLong;
import static co.paralleluniverse.concurrent.util.EnhancedAtomicLong.*;
import co.paralleluniverse.fibers.Suspendable;

/**
 *
 * @author pron
 * @author circlespainter
 */
class TakeReceivePort<M> extends TransformingReceivePort<M> {
    private final EnhancedAtomicLong lease = new EnhancedAtomicLong();
    private final AtomicLong countDown = new AtomicLong();
    private final Condition monitor = new SimpleConditionSynchronizer(null);

    public TakeReceivePort(final ReceivePort<M> target, final long count) {
        super(target);

        final long start = count <= 0 ? 0 : count;
        this.lease.set(start);
        this.countDown.set(start);
    }

    @Override
    @Suspendable
    public M tryReceive() {
        try {
            return timedReceive(0, TimeUnit.NANOSECONDS);
        } catch (Throwable t) {
            // It should never happen
            throw new AssertionError(t);
        }
    }

    @Override
    public M receive() throws SuspendExecution, InterruptedException {
        return timedReceive(-1, null);
    }

    @Override
    public M receive(final Timeout timeout) throws SuspendExecution, InterruptedException {
        return timedReceive(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    @Override
    public M receive(final long timeout, final TimeUnit unit) throws SuspendExecution, InterruptedException {
        return timedReceive(timeout, unit);
    }

    private M timedReceive(final long timeout, final TimeUnit unit) throws SuspendExecution, InterruptedException {
        // Register in order to receive wakeup signals when waiting in the monitor
        final Object ticket = monitor.register();

        // Init time bookkeeping in case we have to wait when performing a timed receive
        final long start = timeout > 0 ? System.nanoTime() : 0;
        long left = unit != null ? unit.toNanos(timeout) : 0;
        final long deadline = start + left;
        long now;

        try {
            // Wait loop
            for (int i = 0;; i++) {
                if (isClosed())
                    // Fail
                    return null;

                if (!lease.evalAndUpdate(gt(0), DEC)) { // Front line is busy, wait
                    if (unit == null) // Untimed receive
                        // => Untimed wait
                        monitor.await(i);
                    else if (timeout > 0) { // Timed receive
                        // => Timed wait with time bookkeeping
                        monitor.await(i, left, TimeUnit.NANOSECONDS);
                        now = System.nanoTime();
                        left = deadline - now;
                        if (left <= 0)
                            // Timed receive expired without making it to the front line
                            return null;
                    } else // tryReceive
                        // Front line busy => channel is surely blocking (or closed) => fail try
                        return null;
                } else
                    // Front line has available seats, try receive
                    break;
            }

            // Try receive
            final M ret;
            if (unit == null)
                // Untimed receive
                ret = target.receive();
            else if (timeout > 0)
                // Timed receive
                ret = target.receive(timeout, unit);
            else
                // tryReceive
                ret = target.tryReceive();

            if (ret != null) {
                // Successful receive
                if (countDown.decrementAndGet() <= 0)
                    // Last message consumed, wake up all waiters and return
                    monitor.signalAll();
                return ret;
            } else {
                // Failed receive, let a waiter in and return null
                lease.incrementAndGet();
                monitor.signal();
                return null;
            }
        } finally {
            // No matter what happens, release our ticket in the monitor
            monitor.unregister(ticket);
        }
    }

    @Override
    public boolean isClosed() {
        return countDown.get() <= 0 || super.isClosed();
    }
}
