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
package co.paralleluniverse.strands;

import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class AbstractFuture<V> implements Future<V> {
    private final Condition sync = new SimpleConditionSynchronizer(this);
    private volatile boolean done;
    private volatile int setting;
    private V value;
    private Throwable exception;

    @Override
    public boolean isCancelled() {
        return done && exception instanceof CancellationException;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    protected boolean set(V value) {
        if (done)
            return false;
        if (casSetting(0, 1)) {
            this.value = value;
            this.done = true;
            sync.signalAll();
            return true;
        } else
            return false;
    }

    protected boolean setException(Throwable exception) {
        if (done)
            return false;
        if (casSetting(0, 1)) {
            this.exception = exception;
            this.done = true;
            sync.signalAll();
            return true;
        } else
            return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (done)
            return false;
        if (casSetting(0, 1)) {
            this.exception = new CancellationException();
            this.done = true;
            sync.signalAll();

            if (mayInterruptIfRunning)
                interruptTask();

            return true;
        } else
            return false;
    }

    @Override
    @Suspendable
    public V get() throws InterruptedException, ExecutionException {
        try {
            if (done)
                return getValue();

            Object token = sync.register();
            try {
                for (int i = 0; !done; i++)
                    sync.await(i);
            } finally {
                sync.unregister(token);
            }
            return getValue();
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @Suspendable
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            if (done)
                return getValue();

            long left = unit.toNanos(timeout);
            final long deadline = System.nanoTime() + left;

            Object token = sync.register();
            try {
                for (int i = 0; !done; i++) {
                    sync.await(i, left, TimeUnit.NANOSECONDS);

                    left = deadline - System.nanoTime();
                    if (left <= 0)
                        throw new TimeoutException();
                }
            } finally {
                sync.unregister(token);
            }
            return getValue();
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    private V getValue() throws ExecutionException {
        if (exception != null) {
            if (exception instanceof CancellationException)
                throw (CancellationException) exception;
            throw new ExecutionException(exception);
        }
        return value;
    }

    protected void interruptTask() {
    }
    
    private static final VarHandle SETTING;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            SETTING = l.findVarHandle(AbstractFuture.class, "setting", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private boolean casSetting(int expected, int update) {
        return SETTING.compareAndSet(this, expected, update);
    }
}
