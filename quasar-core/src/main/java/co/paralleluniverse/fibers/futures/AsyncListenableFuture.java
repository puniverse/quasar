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
package co.paralleluniverse.fibers.futures;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.RuntimeExecutionException;
import co.paralleluniverse.fibers.SuspendExecution;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 *
 * @author pron
 */
public class AsyncListenableFuture<V> extends FiberAsync<V, Runnable, Void, ExecutionException> {
    public static <V> V get(ListenableFuture<V> future) throws ExecutionException, InterruptedException, SuspendExecution {
        if (Fiber.currentFiber() != null && !future.isDone())
            return new AsyncListenableFuture<>(future).run();
        else
            return future.get();
    }

    public static <V> V getNoSuspend(final ListenableFuture<V> future) throws ExecutionException, InterruptedException {
        if (Fiber.currentFiber() != null && !future.isDone()) {
            try {
                return new Fiber<V>() {
                    @Override
                    protected V run() throws SuspendExecution, InterruptedException {
                        try {
                            return new AsyncListenableFuture<>(future).run();
                        } catch (ExecutionException e) {
                            throw new RuntimeExecutionException(e.getCause());
                        }
                    }
                }.start().get();
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                if (t instanceof RuntimeExecutionException)
                    throw new ExecutionException(t.getCause());
                else
                    throw e;
            }
        } else
            return future.get();
    }
    
    ///////////////////////////////////////////////////////////////////////
    private final ListenableFuture<V> fut;

    private AsyncListenableFuture(ListenableFuture<V> future) {
        this.fut = future;
    }

    @Override
    protected Runnable getCallback() {
        return null;
    }

    @Override
    protected Void requestAsync(final Fiber fiber, Runnable callback) {
        fut.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    assert fut.isDone();
                    final V res = fut.get();
                    completed(res, fiber);
                } catch (ExecutionException e) {
                    failed(e, fiber);
                } catch (InterruptedException e) {
                    throw new AssertionError(); // can't happen b/c we know future is done.
                }
            }
        }, sameThreadExecutor);
        return null;
    }
    private static final Executor sameThreadExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };
}
