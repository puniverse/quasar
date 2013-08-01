/*
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
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import jsr166e.CompletableFuture;

/**
 *
 * @author pron
 */
/**
 * Implements {@link CompletableExecutorService} execution methods atop the abstract {@link #execute}
 * method. More concretely, the {@code submit}, {@code invokeAny} and {@code invokeAll} methods
 * create {@link CompletableFutureTask} instances and pass them to {@link #execute}.
 *
 * <p>In addition to {@link #execute}, subclasses must implement all methods related to shutdown and
 * termination.
 */
public abstract class AbstractCompletableExecutorService extends AbstractExecutorService implements CompletableExecutorService {
    @Override
    public CompletableFuture<?> submit(Runnable task) {
        return (CompletableFuture<?>) super.submit(task);
    }

    @Override
    public <T> CompletableFuture<T> submit(Runnable task, T result) {
        return (CompletableFuture<T>) super.submit(task, result);
    }

    @Override
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        return (CompletableFuture<T>) super.submit(task);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
        return new CompletableFutureTask<T>(new Callable<T>() {
            @Override
            public T call() {
                runnable.run();
                return value;
            }
        });
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new CompletableFutureTask<T>(callable);
    }
}
