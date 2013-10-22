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
import java.util.concurrent.ExecutionException;
import jsr166e.CompletableFuture;

/**
 *
 * @author pron
 */
public class AsyncCompletableFuture<V> extends FiberAsync<V, Void, ExecutionException> {
    public static <V> V get(CompletableFuture<V> future) throws ExecutionException, InterruptedException, SuspendExecution {
        if (Fiber.currentFiber() != null && !future.isDone())
            return new AsyncCompletableFuture<>(future).run();
        else
            return future.get();
    }

    public static <V> V getNoSuspend(final CompletableFuture<V> future) throws ExecutionException, InterruptedException {
        if (Fiber.currentFiber() != null && !future.isDone()) {
            try {
                return new Fiber<V>() {
                    @Override
                    protected V run() throws SuspendExecution, InterruptedException {
                        try {
                            return new AsyncCompletableFuture<>(future).run();
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
    private final CompletableFuture<V> fut;

    private AsyncCompletableFuture(CompletableFuture<V> future) {
        this.fut = future;
    }

    @Override
    protected Void requestAsync() {
        fut.handle(new CompletableFuture.BiFun<V, Throwable, Void>() {

            @Override
            public Void apply(V res, Throwable e) {
                if(e != null)
                    asyncFailed(e);
                else
                    asyncCompleted(res);
                return null;
            }
        });
        return null;
    }

    @Override
    protected V requestSync() throws InterruptedException, ExecutionException {
        return fut.get();
    }
}
