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
package co.paralleluniverse.strands.dataflow;

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

    @Override
    public V get() throws InterruptedException, SuspendExecution {
        SimpleConditionSynchronizer s;
        while ((s = sync) != null)
            s.await();
        return value;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, SuspendExecution {
        SimpleConditionSynchronizer s;
        while ((s = sync) != null)
            s.await(timeout, unit);
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
