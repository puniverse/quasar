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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Future;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class DelayedVal<V> implements Future<V> {
    private V value;
    private volatile SimpleConditionSynchronizer sync = new SimpleConditionSynchronizer();

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

    @Override
    public V get() throws InterruptedException, SuspendExecution {
        final SimpleConditionSynchronizer s = sync;
        if (s != null) {
            s.register();
            try {
                for (int i = 0; sync != null; i++)
                    s.await(i);
            } finally {
                s.unregister();
            }
        }
        return value;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, SuspendExecution {
        final SimpleConditionSynchronizer s = sync;
        if (s != null) {
            s.register();
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
                s.unregister();
            }
        }
        return value;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
