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
 * A general helper class that transforms asynchronous requests to synchronous calls on a Fiber.
 *
 * @author pron
 * @param <V> The value retuned by the async request
 * @param <Callback> The interface of the async callback.
 * @param <E> An exception class that could be thrown by the async request
 */
public abstract class FiberAsync<V, Callback, A, E extends Throwable> implements Fiber.PostParkActions {
    private final boolean immediateExec;

    public FiberAsync(boolean immediateExec) {
        this.immediateExec = immediateExec;
    }

    public FiberAsync() {
        this(false);
    }

    @SuppressWarnings("empty-statement")
    public V run() throws E, SuspendExecution, InterruptedException {
        while (!Fiber.park(this, this)) // make sure we actually park and run PostParkActions
            ;
        while (!isCompleted())
            Fiber.park((Object)this);
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
    
    protected Callback getCallback() {
        return (Callback)this;
    }
    
    //
    private volatile boolean completed;
    private Throwable exception;
    private V result;
    private A attachment;

    protected void completed(V result, Fiber fiber) {
        this.result = result;
        completed = true;
        fire(fiber);
    }

    protected void failed(Throwable exc, Fiber fiber) {
        this.exception = exc;
        completed = true;
        fire(fiber);
    }

    private void fire(Fiber fiber) {
        if (immediateExec) {
            if(!fiber.exec(this)) {
                final RuntimeException ex = new RuntimeException("Failed to exec fiber " + fiber + " in thread " + Thread.currentThread());
                
                this.exception = ex;
                fiber.unpark();
                
                throw ex;
            }
        } else
            fiber.unpark();
    }

    /**
     * Internal method, do not call. Called by Fiber immediately after park.
     * This method may not use any ThreadLocals as they have been rest by the time the method is called.
     *
     * @param current
     */
    @Override
    public final void run(Fiber current) {
        attachment = requestAsync(current, getCallback());
    }

    protected A getAttachment() {
        return attachment;
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
