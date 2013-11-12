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
 * A general helper class that transforms asynchronous requests to synchronous (fiber-blocking) calls.
 *
 * ### Usage example:
 *
 * Assume that operation `Foo.asyncOp(FooCompletion callback)` is an asynchronous operation, where `Completion` is defined as:
 *
 * ```java
 * interface FooCompletion {
 *    void success(String result);
 *    void failure(FooException exception);
 * }
 * ```
 * We then define the following subclass:
 *
 * ```java
 * class FooAsync extends FiberAsync<String, Void, FooException> implements FooCompletion {
 *     {@literal @}Override
 *     public void success(String result) {
 *         asyncCompleted(result);
 *     }
 *
 *     {@literal @}Override
 *     public void failure(FooException exception) {
 *         asyncFailed(exception);
 *     }
 * }
 * ```
 *
 * Then, to turn the operation into a fiber-blocking one, we can define:
 *
 * ```java
 * String op() {
 *     return new FooAsync() {
 *         protected Void requestAsync() {
 *             Foo.asyncOp(this);
 *         }
 *     }.run();
 * }
 * ```
 *
 * @param <V> The value returned by the async request
 * @param <A> The type of the (optional) attachment object associated with this `FiberAsyc`.
 * @param <E> An exception class that could be thrown by the async request
 *
 * @author pron
 */
public abstract class FiberAsync<V, A, E extends Throwable> {
    private final Fiber fiber;
    private final boolean immediateExec;
    private long timeoutNanos;
    private volatile boolean completed;
    private Throwable exception;
    private V result;
    private Thread registrationThread;
    private volatile boolean registrationComplete;
    private A attachment;

    /**
     * Same as `FiberAsync(false)`
     */
    public FiberAsync() {
        this(false);
    }

    /**
     *
     * @param immediateExec Whether the fiber should be executed in the same thread as the callback. Should generally be set to `false`.
     */
    public FiberAsync(boolean immediateExec) {
        this.fiber = Fiber.currentFiber();
        this.immediateExec = immediateExec;
    }

    /**
     * Runs the asynchronous operation, blocks until it completes and returns its result. Throws an exception if the operation has failed.
     * <p/>
     * In immediate exec mode, when this method returns we are running within the handler, and will need to call {@link Fiber#yield()}
     * to return from the handler.
     *
     * @return the result of the async operation as set in the call to {@link #completed(Object, Fiber) completed}.
     * @throws E if the async computation failed and an exception was set in a call to {@link #failed(Throwable, Fiber) failed}.
     * @throws InterruptedException
     */
    @SuppressWarnings("empty-statement")
    public V run() throws E, SuspendExecution, InterruptedException {
        if (fiber == null)
            return requestSync();

        fiber.record(1, "FiberAsync", "run", "Blocking fiber %s on FibeAsync %s", fiber, this);
        while (!Fiber.park(this, new Fiber.ParkAction() {
            @Override
            public void run(Fiber current) {
                current.record(1, "FiberAsync", "run", "Calling requestAsync on class %s", this);
                registrationThread = Thread.currentThread();
                attachment = requestAsync();
                registrationThread = null;
                registrationComplete = true;
                current.record(1, "FiberAsync", "run", "requestAsync on %s returned attachment %s", FiberAsync.this, attachment);
            }
        })); // make sure we actually park and run PostParkActions

        if (Thread.currentThread() != registrationThread)
            while (!registrationComplete); // spin

        if (Fiber.interrupted())
            throw new InterruptedException();

        assert isCompleted() : "Unblocker: " + Fiber.currentFiber().getUnparker();

//        while (!isCompleted() || (immediateExec && !Fiber.currentFiber().isInExec())) {
//            Fiber.park((Object) this);
//            throw new InterruptedException();
//        }
        return getResult();
    }

    /**
     * Runs the asynchronous operation, blocks until it completes (but only up to the given timeout duration) and returns its result.
     * Throws an exception if the operation has failed.
     * <p/>
     * In immediate exec mode, when this method returns we are running within the handler, and will need to call {@link Fiber#yield()}
     * to return from the handler.
     *
     * @return the result of the async operation as set in the call to {@link #completed(Object, Fiber) completed}.
     * @throws E if the async computation failed and an exception was set in a call to {@link #failed(Throwable, Fiber) failed}.
     * @throws TimeoutException if the operation had not completed by the time the timeout has elapsed.
     * @throws InterruptedException
     */
    @SuppressWarnings("empty-statement")
    public V run(final long timeout, final TimeUnit unit) throws E, SuspendExecution, InterruptedException, TimeoutException {
        if (Fiber.currentFiber() == null)
            return requestSync();
        if (unit == null)
            return run();
        if (timeout <= 0)
            throw new TimeoutException();

        final long deadline = System.nanoTime() + unit.toNanos(timeout);

        fiber.record(1, "FiberAsync", "run", "Blocking fiber %s on FibeAsync %s", fiber, this);
        while (!Fiber.park(this, new Fiber.ParkAction() {
            @Override
            public void run(Fiber current) {
                current.timeoutService.schedule(current, FiberAsync.this, timeout, unit);
                current.record(1, "FiberAsync", "run", "Calling requestAsync on class %s", this);
                registrationThread = Thread.currentThread();
                attachment = requestAsync();
                registrationThread = null;
                registrationComplete = true;
                current.record(1, "FiberAsync", "run", "requestAsync on %s returned attachment %s", FiberAsync.this, attachment);
            }
        })); // make sure we actually park and run PostParkActions

        if (Thread.currentThread() != registrationThread)
            while (!registrationComplete); // spin

        if (!isCompleted()) {
            if (Fiber.interrupted())
                throw new InterruptedException();

            assert System.nanoTime() >= deadline;
            exception = new TimeoutException();
            completed = true;
            fiber.record(1, "FiberAsync", "run", "FibeAsync %s on fiber %s has timed out", this, fiber);
            throw (TimeoutException) exception;
        }

        return getResult();
    }

    /**
     * A user of this class must override this method to start the asynchronous operation and register the callback.
     * This method may return an *attachment object* that can be retrieved later by calling {@link #getAttachment()}.
     * This method may not use any {@link ThreadLocal}s.
     *
     * @return An object to be set as this `FiberAsync`'s *attachment*, that can be later retrieved with {@link #getAttachment()}.
     */
    protected abstract A requestAsync();

    /**
     * Called if {@link #run()} is not being executed in a fiber. Should perform the operation synchronously and return its result.
     * The default implementation of this method throws an `IllegalThreadStateException`.
     *
     * @return The operation's result.
     * @throws E
     * @throws InterruptedException
     */
    protected V requestSync() throws E, InterruptedException {
        throw new IllegalThreadStateException("Method called not from within a fiber");
    }

    /**
     * This method must be called by the callback upon successful completion of the asynchronous operation.
     *
     * @param result The operation's result
     */
    protected final void asyncCompleted(V result) {
        if (completed) // probably timeout
            return;
        this.result = result;
        completed = true;
        // a race can happen at this point in the immediateExec case, hence the test Fiber.currentFiber().isInExec() in run()
        fire(fiber);
    }

    /**
     * This method must be called by the callback upon a failure of the asynchronous operation.
     *
     * @param t The exception that caused the failure, or an exception to be associated with it. Must not be `null`.
     */
    protected final void asyncFailed(Throwable t) {
        if (t == null)
            throw new IllegalArgumentException("t must not be null");
        if (completed) // probably timeout
            return;
        this.exception = t;
        completed = true;
        // a race can happen at this point in the immediateExec case, hence the test Fiber.currentFiber().isInExec() in run()
        fire(fiber);
    }

    private void fire(Fiber fiber) {
//        if (Thread.currentThread() != registrationThread)
//            while (!registrationComplete); // spin
        
        if (immediateExec) {
            fiber.record(1, "FiberAsync", "fire", "%s - Immediate exec of fiber %s", this, fiber);
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
     * Called by the fiber if this `FiberAsync` is in immediate-exec mode, immediately before attempting to block while running
     * in the callback's thread.
     * Can be overridden by subclasses running in immediate-exec mode to verify whether a park is allowed.
     */
    protected void prepark() {
    }

    /**
     * Returns the attachment object that was returned by the call to {@link #requestAsync(Fiber, Object) requestAsync}.
     */
    protected final A getAttachment() {
        return attachment;
    }

    /**
     * Tests whether or not the asynchronous operation represented by this `FiberAsyc` has completed.
     */
    public final boolean isCompleted() {
        return completed;
    }

    /**
     * Returns the result of the asynchronous operation if it has completed, or throws an exception if it completed unsuccessfully.
     * If the operation has not yet completed, this method throws an `IllegalStateException`.
     *
     * @return
     * @throws E if the async computation failed and an exception was set in a call to {@link #failed(Throwable, Fiber) failed}.
     * @throw IllegalStateException if the operation has not yet completed.
     */
    public final V getResult() throws E {
        if (!completed)
            throw new IllegalStateException("Not completed");
        if (exception != null)
            throw (E) exception;
        return result;
    }
}
