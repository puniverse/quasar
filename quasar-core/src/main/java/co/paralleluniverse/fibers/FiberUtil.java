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
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Static utility methods for working with fibers.
 *
 * @author pron
 */
public final class FiberUtil {
    /**
     * Turns a fiber into a {@link Future}.
     *
     * @param <V>
     * @param fiber the fiber
     * @return a {@link Future} representing the fiber.
     */
    public static <V> Future<V> toFuture(Fiber<V> fiber) {
        return fiber;
    }

    /**
     * Runs an action in a new fiber, awaits the fiber's termination, and returns its result.
     * The new fiber is scheduled by the {@link DefaultFiberScheduler default scheduler}.
     *
     * @param <V>
     * @param target the operation
     * @return the operations return value
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static <V> V runInFiber(SuspendableCallable<V> target) throws ExecutionException, InterruptedException {
        return runInFiber(DefaultFiberScheduler.getInstance(), target);
    }

    /**
     * Runs an action in a new fiber, awaits the fiber's termination, and returns its result.
     *
     * @param <V>
     * @param scheduler the {@link FiberScheduler} to use when scheduling the fiber.
     * @param target    the operation
     * @return the operations return value
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static <V> V runInFiber(FiberScheduler scheduler, SuspendableCallable<V> target) throws ExecutionException, InterruptedException {
        return new Fiber<>(scheduler, target).start().get();
    }

    /**
     * Runs an action in a new fiber and awaits the fiber's termination.
     * The new fiber is scheduled by the {@link DefaultFiberScheduler default scheduler}.
     * .
     *
     * @param target the operation
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void runInFiber(SuspendableRunnable target) throws ExecutionException, InterruptedException {
        runInFiber(DefaultFiberScheduler.getInstance(), target);
    }

    /**
     * Runs an action in a new fiber and awaits the fiber's termination.
     *
     * @param scheduler the {@link FiberScheduler} to use when scheduling the fiber.
     * @param target    the operation
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void runInFiber(FiberScheduler scheduler, SuspendableRunnable target) throws ExecutionException, InterruptedException {
        new Fiber<Void>(scheduler, target).start().join();
    }

    /**
     * Runs an action in a new fiber, awaits the fiber's termination, and returns its result.
     * Unlike {@link #runInFiber(SuspendableCallable) runInFiber} this method does not throw {@link ExecutionException}, but wraps
     * any checked exception thrown by the operation in a {@link RuntimeException}.
     * The new fiber is scheduled by the {@link DefaultFiberScheduler default scheduler}.
     *
     * @param <V>
     * @param target the operation
     * @return the operations return value
     * @throws InterruptedException
     */
    public static <V> V runInFiberRuntime(SuspendableCallable<V> target) throws InterruptedException {
        return runInFiberRuntime(DefaultFiberScheduler.getInstance(), target);
    }

    /**
     * Runs an action in a new fiber, awaits the fiber's termination, and returns its result.
     * Unlike {@link #runInFiber(FiberScheduler, SuspendableCallable) runInFiber} this method does not throw {@link ExecutionException}, but wraps
     * any checked exception thrown by the operation in a {@link RuntimeException}.
     *
     * @param <V>
     * @param scheduler the {@link FiberScheduler} to use when scheduling the fiber.
     * @param target    the operation
     * @return the operations return value
     * @throws InterruptedException
     */
    public static <V> V runInFiberRuntime(FiberScheduler scheduler, SuspendableCallable<V> target) throws InterruptedException {
        try {
            return new Fiber<>(scheduler, target).start().get();
        } catch (ExecutionException e) {
            throw Exceptions.rethrow(e.getCause());
        }
    }

    /**
     * Runs an action in a new fiber and awaits the fiber's termination.
     * Unlike {@link #runInFiber(SuspendableRunnable)  runInFiber} this method does not throw {@link ExecutionException}, but wraps
     * any checked exception thrown by the operation in a {@link RuntimeException}.
     * The new fiber is scheduled by the {@link DefaultFiberScheduler default scheduler}.
     *
     * @param target the operation
     * @throws InterruptedException
     */
    public static void runInFiberRuntime(SuspendableRunnable target) throws InterruptedException {
        runInFiberRuntime(DefaultFiberScheduler.getInstance(), target);
    }

    /**
     * Runs an action in a new fiber and awaits the fiber's termination.
     * Unlike {@link #runInFiber(FiberScheduler, SuspendableRunnable)   runInFiber} this method does not throw {@link ExecutionException}, but wraps
     * any checked exception thrown by the operation in a {@link RuntimeException}.
     *
     * @param scheduler the {@link FiberScheduler} to use when scheduling the fiber.
     * @param target    the operation
     * @throws InterruptedException
     */
    public static void runInFiberRuntime(FiberScheduler scheduler, SuspendableRunnable target) throws InterruptedException {
        try {
            new Fiber<Void>(scheduler, target).start().join();
        } catch (ExecutionException e) {
            throw Exceptions.rethrow(e.getCause());
        }
    }

    /**
     * Runs an action in a new fiber, awaits the fiber's termination, and returns its result.
     * Unlike {@link #runInFiber(SuspendableCallable) runInFiber} this method does not throw {@link ExecutionException}, but wraps
     * any checked exception thrown by the operation in a {@link RuntimeException}.
     * The new fiber is scheduled by the {@link DefaultFiberScheduler default scheduler}.
     *
     * @param <V>
     * @param target the operation
     * @return the operations return value
     * @throws InterruptedException
     */
    public static <V, X extends Exception> V runInFiberChecked(SuspendableCallable<V> target, Class<X> exceptionType) throws X, InterruptedException {
        return runInFiberChecked(DefaultFiberScheduler.getInstance(), target, exceptionType);
    }

    /**
     * Runs an action in a new fiber, awaits the fiber's termination, and returns its result.
     * Unlike {@link #runInFiber(FiberScheduler, SuspendableCallable) runInFiber} this method does not throw {@link ExecutionException}, but wraps
     * any checked exception thrown by the operation in a {@link RuntimeException}, unless it is of the given {@code exception type}, in
     * which case the checked exception is thrown as-is.
     *
     * @param <V>
     * @param scheduler     the {@link FiberScheduler} to use when scheduling the fiber.
     * @param target        the operation
     * @param exceptionType a checked exception type that will not be wrapped if thrown by the operation, but thrown as-is.
     * @return the operations return value
     * @throws InterruptedException
     */
    public static <V, X extends Exception> V runInFiberChecked(FiberScheduler scheduler, SuspendableCallable<V> target, Class<X> exceptionType) throws X, InterruptedException {
        try {
            return new Fiber<>(scheduler, target).start().get();
        } catch (ExecutionException ex) {
            throw throwChecked(ex, exceptionType);
        }
    }

    /**
     * Runs an action in a new fiber and awaits the fiber's termination.
     * Unlike {@link #runInFiber(SuspendableRunnable)  runInFiber} this method does not throw {@link ExecutionException}, but wraps
     * any checked exception thrown by the operation in a {@link RuntimeException}, unless it is of the given {@code exception type}, in
     * which case the checked exception is thrown as-is.
     * The new fiber is scheduled by the {@link DefaultFiberScheduler default scheduler}.
     *
     * @param target        the operation
     * @param exceptionType a checked exception type that will not be wrapped if thrown by the operation, but thrown as-is.
     * @throws InterruptedException
     */
    public static <X extends Exception> void runInFiberChecked(SuspendableRunnable target, Class<X> exceptionType) throws X, InterruptedException {
        runInFiberChecked(DefaultFiberScheduler.getInstance(), target, exceptionType);
    }

    /**
     * Runs an action in a new fiber and awaits the fiber's termination.
     * Unlike {@link #runInFiber(SuspendableRunnable)  runInFiber} this method does not throw {@link ExecutionException}, but wraps
     * any checked exception thrown by the operation in a {@link RuntimeException}, unless it is of the given {@code exception type}, in
     * which case the checked exception is thrown as-is.
     *
     * @param scheduler     the {@link FiberScheduler} to use when scheduling the fiber.
     * @param target        the operation
     * @param exceptionType a checked exception type that will not be wrapped if thrown by the operation, but thrown as-is.
     * @throws InterruptedException
     */
    public static <X extends Exception> void runInFiberChecked(FiberScheduler scheduler, SuspendableRunnable target, Class<X> exceptionType) throws X, InterruptedException {
        try {
            new Fiber<Void>(scheduler, target).start().join();
        } catch (ExecutionException ex) {
            throw throwChecked(ex, exceptionType);
        }
    }

    /**
     * Blocks on the input fibers and creates a new list from the results. The result list is the same order as the
     * input list.
     *
     * @param fibers to combine
     */
    public static <V> List<V> get(final List<Fiber<V>> fibers) throws InterruptedException {
        final List<V> results = new ArrayList<>(fibers.size());

        //TODO on interrupt, should all input fibers be canceled?
        for (final Fiber<V> f : fibers) {
            try {
                results.add(f.get());
            } catch (ExecutionException e) {
                throw Exceptions.rethrowUnwrap(e);
            }
        }

        return Collections.unmodifiableList(results);
    }

    /**
     * Blocks on the input fibers and creates a new list from the results. The result list is the same order as the
     * input list.
     *
     * @param fibers to combine
     */
    public static <V> List<V> get(Fiber<V>... fibers) throws InterruptedException {
        return get(Arrays.asList(fibers));
    }

    /**
     * Blocks on the input fibers and creates a new list from the results. The result list is the same order as the
     * input list.
     *
     * @param timeout to wait for all requests to complete
     * @param unit    the time is in
     * @param fibers  to combine
     */
    public static <V> List<V> get(long timeout, TimeUnit unit, List<Fiber<V>> fibers) throws InterruptedException, TimeoutException {
        if (unit == null)
            return get(fibers);
        if (timeout < 0)
            timeout = 0;

        final List<V> results = new ArrayList<>(fibers.size());

        long left = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + left;

        //TODO on interrupt, should all input fibers be canceled?
        try {
            for (final Fiber<V> f : fibers) {
                if (left >= 0) {
                    results.add(f.get(left, TimeUnit.NANOSECONDS));
                    left = deadline - System.nanoTime();
                } else
                    throw new TimeoutException("timed out sequencing fiber results");
            }
            return Collections.unmodifiableList(results);
        } catch (ExecutionException e) {
            throw Exceptions.rethrowUnwrap(e);
        }
    }

    /**
     * Blocks on the input fibers and creates a new list from the results. The result list is the same order as the
     * input list.
     *
     * @param time   to wait for all requests to complete
     * @param unit   the time is in
     * @param fibers to combine
     */
    public static <V> List<V> get(final long time, final TimeUnit unit, final Fiber<V>... fibers) throws InterruptedException, TimeoutException {
        return get(time, unit, Arrays.asList(fibers));
    }

    private static <V, X extends Exception> RuntimeException throwChecked(ExecutionException ex, Class<X> exceptionType) throws X {
        Throwable t = Exceptions.unwrap(ex);
        if (exceptionType.isInstance(t))
            throw exceptionType.cast(t);
        else
            throw Exceptions.rethrow(t);
    }

    private FiberUtil() {
    }
}
