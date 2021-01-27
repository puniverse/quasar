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
package co.paralleluniverse.fibers.futures;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Turns {@link CompletableFuture}s into fiber-blocking operations.
 *
 * @author pron
 */
public class AsyncCompletionStage<V> extends FiberAsync<V, ExecutionException> {
    /**
     * Blocks the current strand (either fiber or thread) until the given future completes, and returns its result.
     *
     * @param future the future
     * @return the future's result
     * @throws ExecutionException   if the future's computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    public static <V> V get(CompletionStage<V> future) throws ExecutionException, InterruptedException, SuspendExecution {
        if (Fiber.isCurrentFiber())
            return new AsyncCompletionStage<>(future).run();
        else
            return future.toCompletableFuture().get();
    }

    /**
     * Blocks the current strand (either fiber or thread) until the given future completes - but no longer than the given timeout - and returns its result.
     *
     * @param future  the future
     * @param timeout the maximum duration to wait for the future's result
     * @param unit    the timeout's time unit
     * @return the future's result
     * @throws ExecutionException   if the future's computation threw an exception
     * @throws TimeoutException     if the timeout expired before the future completed
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    public static <V> V get(CompletionStage<V> future, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, SuspendExecution, TimeoutException {
        if (Fiber.isCurrentFiber())
            return new AsyncCompletionStage<>(future).run(timeout, unit);
        else
            return future.toCompletableFuture().get(timeout, unit);
    }

    /**
     * Blocks the current strand (either fiber or thread) until the given future completes - but no longer than the given timeout - and returns its result.
     *
     * @param future  the future
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return the future's result
     * @throws ExecutionException   if the future's computation threw an exception
     * @throws TimeoutException     if the timeout expired before the future completed
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    public static <V> V get(CompletionStage<V> future, Timeout timeout) throws ExecutionException, InterruptedException, SuspendExecution, TimeoutException {
        return get(future, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

//    /**
//     * Blocks the current strand (either fiber or thread) until the given future completes, and returns its result.
//     * <p/>
//     * Unlike {@link #get(CompletionStage) get}, while this is a fiber-blocking operation, it is not suspendable. It blocks the fiber
//     * by other, less efficient means, and {@link #get(CompletableFuture) get} should be generally preferred over this method.
//     *
//     * @param future the future
//     * @return the future's result
//     * @throws ExecutionException   if the future's computation threw an exception
//     * @throws InterruptedException if the current thread was interrupted while waiting
//     */
//    public static <V> V getNoSuspend(final CompletableFuture<V> future) throws ExecutionException, InterruptedException {
//        if (Fiber.isCurrentFiber() && !future.isDone()) {
//            try {
//                return new Fiber<V>() {
//                    @Override
//                    protected V run() throws SuspendExecution, InterruptedException {
//                        try {
//                            return new AsyncCompletionStage<>(future).run();
//                        } catch (ExecutionException e) {
//                            throw new RuntimeExecutionException(e.getCause());
//                        }
//                    }
//                }.start().get();
//            } catch (ExecutionException e) {
//                Throwable t = e.getCause();
//                if (t instanceof RuntimeExecutionException)
//                    throw new ExecutionException(t.getCause());
//                else
//                    throw e;
//            }
//        } else
//            return future.get();
//    }
//
//    /**
//     * Blocks the current strand (either fiber or thread) until the given future completes - but no longer than the given timeout - and returns its result.
//     * <p/>
//     * Unlike {@link #get(CompletableFuture, long, TimeUnit)  get}, while this is a fiber-blocking operation, it is not suspendable. It blocks the fiber
//     * by other, less efficient means, and {@link #get(CompletableFuture, long, TimeUnit) get} should be generally preferred over this method.
//     *
//     * @param future  the future
//     * @param timeout the maximum duration to wait for the future's result
//     * @param unit    the timeout's time unit
//     * @return the future's result
//     * @throws ExecutionException   if the future's computation threw an exception
//     * @throws TimeoutException     if the timeout expired before the future completed
//     * @throws InterruptedException if the current thread was interrupted while waiting
//     */
//    public static <V> V getNoSuspend(final CompletableFuture<V> future, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
//        if (Fiber.isCurrentFiber() && !future.isDone()) {
//            try {
//                return new Fiber<V>() {
//                    @Override
//                    protected V run() throws SuspendExecution, InterruptedException {
//                        try {
//                            return new AsyncCompletionStage<>(future).run();
//                        } catch (ExecutionException e) {
//                            throw new RuntimeExecutionException(e.getCause());
//                        }
//                    }
//                }.start().get(timeout, unit);
//            } catch (ExecutionException e) {
//                Throwable t = e.getCause();
//                if (t instanceof RuntimeExecutionException)
//                    throw new ExecutionException(t.getCause());
//                else
//                    throw e;
//            }
//        } else
//            return future.get(timeout, unit);
//    }
//
//    /**
//     * Blocks the current strand (either fiber or thread) until the given future completes - but no longer than the given timeout - and returns its result.
//     * <p/>
//     * Unlike {@link #get(ListenableFuture, long, TimeUnit)  get}, while this is a fiber-blocking operation, it is not suspendable. It blocks the fiber
//     * by other, less efficient means, and {@link #get(ListenableFuture, long, TimeUnit) get} should be generally preferred over this method.
//     *
//     * @param future  the future
//     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
//     * @return the future's result
//     * @throws ExecutionException   if the future's computation threw an exception
//     * @throws TimeoutException     if the timeout expired before the future completed
//     * @throws InterruptedException if the current thread was interrupted while waiting
//     */
//    public static <V> V getNoSuspend(final CompletableFuture<V> future, Timeout timeout) throws ExecutionException, InterruptedException, TimeoutException {
//        return getNoSuspend(future, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
//    }
    ///////////////////////////////////////////////////////////////////////
    private final CompletionStage<V> fut;

    private AsyncCompletionStage(CompletionStage<V> future) {
        this.fut = future;
    }

    @Override
    protected void requestAsync() {
        fut.handle((V res, Throwable e) -> {
                if (e != null)
                    asyncFailed(e);
                else
                    asyncCompleted(res);
                return null;
            });
    }

    @Override
    protected ExecutionException wrapException(Throwable t) {
        return new ExecutionException(t);
    }
    
    @Override
    protected V requestSync() throws InterruptedException, ExecutionException {
        return fut.toCompletableFuture().get();
    }
}
