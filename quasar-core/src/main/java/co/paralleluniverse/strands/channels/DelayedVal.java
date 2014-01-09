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
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents a delayed value that can be set at most once, and when read, blocks until a value has been set.
 *
 * @author pron
 */
public class DelayedVal<V> implements Future<V> {
    private V value;
    private volatile SimpleConditionSynchronizer sync = new SimpleConditionSynchronizer(this);

    /**
     * Sets the value
     *
     * @param value the value
     * @throws IllegalStateException if the value has already been set.
     */
    public final void set(V value) {
        if (sync == null)
            throw new IllegalStateException("Value has already been set (and can only be set once)");
        this.value = value;
        final SimpleConditionSynchronizer s = sync;
        sync = null; // must be done before signal
        s.signalAll();
    }

    @Override
    public boolean isDone() {
        return sync == null;
    }

    SimpleConditionSynchronizer getSync() {
        return sync;
    }

    V getValue() {
        return value;
    }

    /**
     * Returns the delayed value, blocking until it has been set.
     *
     * @return the value
     * @throws InterruptedException
     */
    @Override
    @Suspendable
    public V get() throws InterruptedException {
        try {
            final SimpleConditionSynchronizer s = sync;
            if (s != null) {
                Object token = s.register();
                try {
                    for (int i = 0; sync != null; i++)
                        s.await(i);
                } finally {
                    s.unregister(token);
                }
            }
            return value;
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns the delayed value, blocking until it has been set, but no longer than the given timeout.
     *
     * @param timeout The maximum duration to block waiting for the value to be set.
     * @param unit    The time unit of the timeout value.
     * @return the value
     * @throws TimeoutException     if the timeout expires before the value is set.
     * @throws InterruptedException
     */
    @Override
    @Suspendable
    public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            final SimpleConditionSynchronizer s = sync;
            if (s != null) {
                Object token = s.register();
                try {
                    final long start = System.nanoTime();
                    long left = unit.toNanos(timeout);
                    final long deadline = start + left;
                    for (int i = 0; sync != null; i++) {
                        s.awaitNanos(i, left);
                        left = deadline - System.nanoTime();
                        if (left <= 0)
                            throw new TimeoutException();
                    }
                } finally {
                    s.unregister(token);
                }
            }
            return value;
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    public V get(Timeout timeout) throws InterruptedException, TimeoutException {
        return get(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    /**
     * Throws {@code UnsupportedOperationException}.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
