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

/**
 *
 * @author pron
 */
public final class FiberUtil {
    public static <V> Future<V> toFuture(Fiber<V> fiber) {
        return fiber;
    }

    public static <V> V runInFiber(SuspendableCallable<V> target) throws ExecutionException, InterruptedException {
        return runInFiber(DefaultFiberScheduler.getInstance(), target);
    }

    public static <V> V runInFiber(FiberScheduler scheduler, SuspendableCallable<V> target) throws ExecutionException, InterruptedException {
        return new Fiber<V>(scheduler, target).start().get();
    }

    public static void runInFiber(SuspendableRunnable target) throws ExecutionException, InterruptedException {
        runInFiber(DefaultFiberScheduler.getInstance(), target);
    }

    public static void runInFiber(FiberScheduler scheduler, SuspendableRunnable target) throws ExecutionException, InterruptedException {
        new Fiber<Void>(scheduler, target).start().join();
    }

    public static <V> V runInFiberRuntime(SuspendableCallable<V> target) throws InterruptedException {
        return runInFiberRuntime(DefaultFiberScheduler.getInstance(), target);
    }

    public static <V> V runInFiberRuntime(FiberScheduler scheduler, SuspendableCallable<V> target) throws InterruptedException {
        try {
            return new Fiber<V>(scheduler, target).start().get();
        } catch (ExecutionException e) {
            throw Exceptions.rethrow(e.getCause());
        }
    }

    public static void runInFiberRuntime(SuspendableRunnable target) throws InterruptedException {
        runInFiberRuntime(DefaultFiberScheduler.getInstance(), target);
    }

    public static void runInFiberRuntime(FiberScheduler scheduler, SuspendableRunnable target) throws InterruptedException {
        try {
            new Fiber<Void>(scheduler, target).start().join();
        } catch (ExecutionException e) {
            throw Exceptions.rethrow(e.getCause());
        }
    }

    public static <V, X extends Exception> V runInFiberChecked(SuspendableCallable<V> target, Class<X> exceptionType) throws X, InterruptedException {
        return runInFiberChecked(DefaultFiberScheduler.getInstance(), target, exceptionType);
    }

    public static <V, X extends Exception> V runInFiberChecked(FiberScheduler scheduler, SuspendableCallable<V> target, Class<X> exceptionType) throws X, InterruptedException {
        try {
            return new Fiber<V>(scheduler, target).start().get();
        } catch (ExecutionException ex) {
            throw throwChecked(ex, exceptionType);
        }
    }

    public static <X extends Exception> void runInFiberChecked(SuspendableRunnable target, Class<X> exceptionType) throws X, InterruptedException {
        runInFiberChecked(DefaultFiberScheduler.getInstance(), target, exceptionType);
    }

    public static <X extends Exception> void runInFiberChecked(FiberScheduler scheduler, SuspendableRunnable target, Class<X> exceptionType) throws X, InterruptedException {
        try {
            new Fiber<Void>(scheduler, target).start().join();
        } catch (ExecutionException ex) {
            throw throwChecked(ex, exceptionType);
        }
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
