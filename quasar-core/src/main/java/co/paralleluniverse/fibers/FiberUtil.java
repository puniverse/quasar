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
        return new Fiber<V>(target).start().get();
    }

    public static <V> V runInFiber(ForkJoinPool fjPool, SuspendableCallable<V> target) throws ExecutionException, InterruptedException {
        return new Fiber<V>(fjPool, target).start().get();
    }

    public static <V, X extends Exception> V runInFiberChecked(SuspendableCallable<V> target, Class<X> exceptionType) throws X, InterruptedException {
        try {
            return new Fiber<V>(target).start().get();
        } catch (ExecutionException ex) {
            throw throwChecked(ex, exceptionType);
        }
    }

    public static <V, X extends Exception> V runInFiberChecked(ForkJoinPool fjPool, SuspendableCallable<V> target, Class<X> exceptionType) throws X, InterruptedException {
        try {
            return new Fiber<V>(fjPool, target).start().get();
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
