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

/**
 * A general helper class that transforms asynchronous requests to synchronous calls on a LightweightThread.
 *
 * @author pron
 * @param <V> The value retuned by the async request
 * @param <Callback> The interface of the async callback.
 * @param <E> An exception class that could be thrown by the async request
 */
public abstract class FiberAsync<V, Callback, E extends Throwable> implements Fiber.PostParkActions {
    @SuppressWarnings("empty-statement")
    public V run() throws E, SuspendExecution {
        while (!Fiber.park(this, this)) // make sure we actually park and run PostParkActions
            ;
        while (!isCompleted())
            Fiber.park(this);
        return getResult();
    }

    /**
     * Calls the asynchronous request and registers the callback.
     * This method may not use any ThreadLocals.
     * 
     * @param current
     * @param callback
     */
    protected abstract void requestAsync(Fiber current, Callback callback);

    private volatile boolean completed;
    private Throwable exception;
    private V result;

    protected void completed(V result, Fiber lwthread) {
        this.result = result;
        completed = true;
        lwthread.unpark();
    }

    protected void failed(Throwable exc, Fiber lwthread) {
        this.exception = exc;
        completed = true;
        lwthread.unpark();
    }

    /**
     * Internal method, do not call. Called by Fiber immediately after park.
     * This method may not use any ThreadLocals as they have been rest by the time the method is called. 
     * @param current
     */
    @Override
    public final void run(Fiber current) {
        requestAsync(current, (Callback) this);
    }

    public boolean isCompleted() {
        return completed;
    }

    public V getResult() throws E {
        if (!completed)
            throw new IllegalStateException("Not completed");
        if (exception != null)
            throw (E) exception;
        return result;
    }
}
