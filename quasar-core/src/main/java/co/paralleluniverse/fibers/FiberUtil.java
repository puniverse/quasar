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

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public final class FiberUtil {
    public static <V> Future<V> toFuture(Fiber<V> fiber) {
        return fiber;
    }

    public static <V> V runInFiber(SuspendableCallable<V> target) throws ExecutionException, InterruptedException {
        return runInFiber(DefaultFiberPool.getInstance(), target);
    }

    public static <V> V runInFiber(ForkJoinPool fjPool, SuspendableCallable<V> target) throws ExecutionException, InterruptedException {
        return new Fiber<V>(fjPool, target).start().get();
    }

    public static void runInFiber(SuspendableRunnable target) throws ExecutionException, InterruptedException {
        runInFiber(DefaultFiberPool.getInstance(), target);
    }

    public static void runInFiber(ForkJoinPool fjPool, SuspendableRunnable target) throws ExecutionException, InterruptedException {
        new Fiber<Void>(fjPool, target).start().join();
    }

    public static <V> V runInFiberRuntime(SuspendableCallable<V> target) throws InterruptedException {
        return runInFiberRuntime(DefaultFiberPool.getInstance(), target);
    }

    public static <V> V runInFiberRuntime(ForkJoinPool fjPool, SuspendableCallable<V> target) throws InterruptedException {
        try {
            return new Fiber<V>(fjPool, target).start().get();
        } catch (ExecutionException e) {
            throw Exceptions.rethrow(e.getCause());
        }
    }

    public static void runInFiberRuntime(SuspendableRunnable target) throws InterruptedException {
        runInFiberRuntime(DefaultFiberPool.getInstance(), target);
    }

    public static void runInFiberRuntime(ForkJoinPool fjPool, SuspendableRunnable target) throws InterruptedException {
        try {
            new Fiber<Void>(fjPool, target).start().join();
        } catch (ExecutionException e) {
            throw Exceptions.rethrow(e.getCause());
        }
    }

    public static <V, X extends Exception> V runInFiberChecked(SuspendableCallable<V> target, Class<X> exceptionType) throws X, InterruptedException {
        return runInFiberChecked(DefaultFiberPool.getInstance(), target, exceptionType);
    }

    public static <V, X extends Exception> V runInFiberChecked(ForkJoinPool fjPool, SuspendableCallable<V> target, Class<X> exceptionType) throws X, InterruptedException {
        try {
            return new Fiber<V>(fjPool, target).start().get();
        } catch (ExecutionException ex) {
            throw throwChecked(ex, exceptionType);
        }
    }

    public static <X extends Exception> void runInFiberChecked(SuspendableRunnable target, Class<X> exceptionType) throws X, ExecutionException, InterruptedException {
        runInFiberChecked(DefaultFiberPool.getInstance(), target, exceptionType);
    }

    public static <X extends Exception> void runInFiberChecked(ForkJoinPool fjPool, SuspendableRunnable target, Class<X> exceptionType) throws X, ExecutionException, InterruptedException {
        try {
            new Fiber<Void>(fjPool, target).start().join();
        } catch (ExecutionException ex) {
            throw throwChecked(ex, exceptionType);
        }
    }

    private static <V, X extends Exception> RuntimeException throwChecked(ExecutionException ex, Class<X> exceptionType) throws X {
        final Throwable t = ex.getCause();
        if (exceptionType.isInstance(t))
            throw (X) t;
        else
            throw Exceptions.rethrow(t);
    }

    private FiberUtil() {
    }
}
