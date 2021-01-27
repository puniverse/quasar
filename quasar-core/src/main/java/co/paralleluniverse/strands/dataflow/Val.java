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
package co.paralleluniverse.strands.dataflow;

import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.RuntimeExecutionException;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A dataflow constant.
 * Represents a delayed value that can be set at most once, and when read, blocks until a value has been set.
 *
 * @author pron
 */
public class Val<V> implements Future<V> {
    private V value;
    private Throwable t;
    private SuspendableCallable<V> f;
    private volatile SimpleConditionSynchronizer sync = new SimpleConditionSynchronizer(this);

    /**
     * Creates a {@code Val} whose value will be the one returned by the given {@link SuspendableCallable}, which will be spawned
     * into a new fiber.
     * <p>
     * @param f The function that will compute this {@code Val}'s value in a newly spawned fiber
     */
    public Val(final SuspendableCallable<V> f) {
        this(DefaultFiberScheduler.getInstance(), f);
    }

    /**
     * Creates a {@code Val} whose value will be the one returned by the given {@link SuspendableCallable}, which will be spawned
     * into a new fiber, scheduled by the given {@link FiberScheduler}.
     * <p>
     * @param scheduler the scheduler in which the new fiber will be spawned.
     * @param f         The function that will compute this {@code Val}'s value in a newly spawned fiber
     */
    public Val(FiberScheduler scheduler, final SuspendableCallable<V> f) {
        this.f = f;
        if (f != null)
            new Fiber<Void>(scheduler, new SuspendableRunnable() {

                @Override
                public void run() throws SuspendExecution {
                    try {
                        Val.this.set0(f.run());
                    } catch (Throwable t) {
                        Val.this.setException0(t);
                    }
                }
            }).start();
    }

    public Val() {
        this(null);
    }

    /**
     * Sets the value. If the value has already been set (or if a function has been supplied to the constructor),
     * this method will throw an {@code IllegalStateException}. However, you should not rely on this behavior,
     * as the implementation is free to silently ignore additional attempts to set the value.
     *
     * @param value the value
     * @throws IllegalStateException if the value has already been set.
     */
    public final void set(V value) {
        if (f != null)
            throw new IllegalStateException("Cannot set a value because a function has been set");
        set0(value);
    }

    /**
     * Sets an exception that will be thrown by {@code get}, wrapped by {@link RuntimeExecutionException}.
     *
     * @param t the exception
     * @throws IllegalStateException if the value has already been set.
     */
    public final void setException(Throwable t) {
        if (f != null)
            throw new IllegalStateException("Cannot set a value because a function has been set");
        setException0(t);
    }

    private void set0(V value) {
        final SimpleConditionSynchronizer s = sync;
        if (s == null)
            throw new IllegalStateException("Value has already been set (and can only be set once)");
        this.value = value;
        sync = null; // must be done before signal
        this.f = null;
        s.signalAll();
    }

    private void setException0(Throwable t) {
        final SimpleConditionSynchronizer s = sync;
        if (s == null)
            throw new IllegalStateException("Value has already been set (and can only be set once)");
        this.t = t;
        sync = null; // must be done before signal
        this.f = null;
        s.signalAll();
    }

    @Override
    public boolean isDone() {
        return sync == null;
    }

    protected SimpleConditionSynchronizer getSync() {
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
            if (t != null)
                throw new RuntimeExecutionException(t);
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
                        if (sync == null)
                            break;
                        left = deadline - System.nanoTime();
                        if (left <= 0)
                            throw new TimeoutException();
                    }
                } finally {
                    s.unregister(token);
                }
            }
            if (t != null)
                throw t instanceof CancellationException ? (CancellationException) t : new RuntimeExecutionException(t);
            return value;
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    @Suspendable
    public V get(Timeout timeout) throws InterruptedException, TimeoutException {
        return get(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    /**
     * Throws {@code UnsupportedOperationException}.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        try {
            setException0(new CancellationException());
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean isCancelled() {
        return t instanceof CancellationException;
    }
}
