/*
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
package co.paralleluniverse.concurrent.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

/**
 *
 * @author pron
 */
public interface CompletableExecutorService extends ExecutorService {
    /**
     * @return a {@code CompletableFuture} representing pending completion of the task
     * @throws RejectedExecutionException {@inheritDoc}
     */
    @Override
    <T> CompletableFuture<T> submit(Callable<T> task);

    /**
     * @return a {@code CompletableFuture} representing pending completion of the task
     * @throws RejectedExecutionException {@inheritDoc}
     */
    @Override
    CompletableFuture<?> submit(Runnable task);

    /**
     * @return a {@code CompletableFuture} representing pending completion of the task
     * @throws RejectedExecutionException {@inheritDoc}
     */
    @Override
    <T> CompletableFuture<T> submit(Runnable task, T result);

    /**
     * {@inheritDoc}
     *
     * <p>All elements in the returned list must be {@link CompletableFuture} instances.
     *
     * @return A list of {@code CompletableFuture} instances representing the tasks, in the same
     * sequential order as produced by the iterator for the given task list, each of which has
     * completed.
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException if any task is null
     */
    @Override
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException;

    /**
     * {@inheritDoc}
     *
     * <p>All elements in the returned list must be {@link CompletableFuture} instances.
     *
     * @return a list of {@code CompletableFuture} instances representing the tasks, in the same
     * sequential order as produced by the iterator for the given task list. If the operation
     * did not time out, each task will have completed. If it did time out, some of these
     * tasks will not have completed.
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException if any task is null
     */
    @Override
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException;
}
