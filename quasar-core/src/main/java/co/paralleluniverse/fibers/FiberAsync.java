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

    /**
     *
     * <p/>
     * In immediate exec mode, when this method returns we are running within the handler, and will need to call Fiber.yield()
     * to return from the handler.
     *
     * @return
     * @throws E
     * @throws SuspendExecution
     * @throws InterruptedException
     */
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

        if (Fiber.interrupted())
            throw new InterruptedException();

        assert isCompleted() : "Unblocker: " + Fiber.currentFiber().getUnparker();

//        while (!isCompleted() || (immediateExec && !Fiber.currentFiber().isInExec())) {
//            Fiber.park((Object) this);
//            throw new InterruptedException();
//        }
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
                current.timeoutService.schedule(current, FiberAsync.this, timeout, unit);
                attachment = requestAsync(current, getCallback());
            }
        })); // make sure we actually park and run PostParkActions

        if (!isCompleted()) {
            if (Fiber.interrupted())
                throw new InterruptedException();
            
            assert System.nanoTime() >= deadline;
            exception = new TimeoutException();
            completed = true;
            throw (TimeoutException) exception;
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
        if (completed) // probably timeout
            return;
        this.result = result;
        completed = true;
        // a race can happen at this point in the immediateExec case, hence the test Fiber.currentFiber().isInExec() in run()
        fire(fiber);
    }

    protected final void failed(Throwable t, Fiber fiber) {
        if (completed) // probably timeout
            return;
        this.exception = t;
        completed = true;
        // a race can happen at this point in the immediateExec case, hence the test Fiber.currentFiber().isInExec() in run()
        fire(fiber);
    }

    private void fire(Fiber fiber) {
        if (immediateExec) {
            if (!fiber.exec(this, new Fiber.ParkAction() {
                public void run(Fiber current) {
                    prepark();
                }
            })) {
                final RuntimeException ex1 = new RuntimeException("Failed to exec fiber " + fiber + " in thread " + Thread.currentThread());

                this.exception = timeoutNanos > 0 ? new TimeoutException() : ex1;
                fiber.unpark(this);
                throw ex1;
            }
        } else
            fiber.unpark(this);
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
