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
package co.paralleluniverse.fibers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A general helper class that transforms asynchronous requests to synchronous calls on a Fiber.
 *
 * @author pron
 * @param <V> The value retuned by the async request
 * @param <Callback> The interface of the async callback.
 * @param <E> An exception class that could be thrown by the async request
 */
public abstract class FiberAsync<V, Callback, A, E extends Throwable> {
    private static final long IMMEDIATE_EXEC_MAX_TIMEOUT = TimeUnit.MILLISECONDS.toNanos(1000);
    private final boolean immediateExec;
    private long timeoutNanos;

    public FiberAsync(boolean immediateExec) {
        this.immediateExec = immediateExec;
    }

    public FiberAsync() {
        this(false);
    }

    @SuppressWarnings("empty-statement")
    public V run() throws E, SuspendExecution, InterruptedException {
        if (Fiber.currentFiber() == null)
            return requestSync();

        while (!Fiber.park(this, new Fiber.ParkAction() {
            @Override
            public void run(Fiber current) {
                attachment = requestAsync(current, getCallback());
            }
        })); // make sure we actually park and run PostParkActions

        while (!isCompleted())
            Fiber.park((Object) this);

        return getResult();
    }

    @SuppressWarnings("empty-statement")
    public V run(final long timeout, final TimeUnit unit) throws E, SuspendExecution, InterruptedException, TimeoutException {
        if (Fiber.currentFiber() == null)
            return requestSync();
        if (unit == null)
            return run();
        if (timeout <= 0)
            throw new TimeoutException();


        final long deadline = System.nanoTime() + unit.toNanos(timeout);

        while (!Fiber.park(this, new Fiber.ParkAction() {
            @Override
            public void run(Fiber current) {
                current.timeoutService.schedule(current, timeout, unit);
                attachment = requestAsync(current, getCallback());
            }
        })); // make sure we actually park and run PostParkActions

        long left;
        while (!isCompleted()) {
            left = deadline - System.nanoTime();
            if (left <= 0)
                throw new TimeoutException();
            Fiber.park((Object) this, left, TimeUnit.NANOSECONDS);
        }

        return getResult();
    }

    /**
     * Calls the asynchronous request and registers the callback.
     * This method may not use any ThreadLocals.
     *
     * @param current
     * @param callback
     */
    protected abstract A requestAsync(Fiber current, Callback callback);

    protected V requestSync() throws E, InterruptedException {
        throw new IllegalThreadStateException("Method called not from within a fiber");
    }

    protected Callback getCallback() {
        return (Callback) this;
    }
    //
    private volatile boolean completed;
    private Throwable exception;
    private V result;
    private A attachment;

    protected final void completed(V result, Fiber fiber) {
        this.result = result;
        completed = true;
        fire(fiber);
    }

    protected final void failed(Throwable exc, Fiber fiber) {
        this.exception = exc;
        completed = true;
        fire(fiber);
    }

    private void fire(Fiber fiber) {
        if (immediateExec) {
            if (!fiber.exec(this, new Fiber.ParkAction() {
                @Override
                public void run(Fiber current) {
                    prepark();
                }
            }, timeoutNanos > 0 ? timeoutNanos : IMMEDIATE_EXEC_MAX_TIMEOUT, TimeUnit.NANOSECONDS)) {
                final RuntimeException ex1 = new RuntimeException("Failed to exec fiber " + fiber + " in thread " + Thread.currentThread());

                this.exception = timeoutNanos > 0 ? new TimeoutException() : ex1;
                fiber.unpark();

                throw ex1;
            }
        } else
            fiber.unpark();
    }

    /**
     * Can be overridden by subclasses running in immediate-exec mode to verify whether a park is allowed.
     *
     * @return
     */
    protected void prepark() {
    }

    protected final A getAttachment() {
        return attachment;
    }

    public final boolean isCompleted() {
        return completed;
    }

    public final V getResult() throws E {
        if (!completed)
            throw new IllegalStateException("Not completed");
        if (exception != null)
            throw (E) exception;
        return result;
    }
}
