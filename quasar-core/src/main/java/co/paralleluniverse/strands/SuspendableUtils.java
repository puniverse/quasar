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
package co.paralleluniverse.strands;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.instrument.DontInstrument;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

import java.util.concurrent.Callable;

/**
 *
 * @author pron
 */
public class SuspendableUtils {
    public static SuspendableCallable<Void> runnableToCallable(SuspendableRunnable runnable) {
        return new VoidSuspendableCallable(runnable);
    }

    public static class VoidSuspendableCallable implements SuspendableCallable<Void> {
        private final SuspendableRunnable runnable;

        public VoidSuspendableCallable(SuspendableRunnable runnable) {
            if (runnable == null)
                throw new NullPointerException("Runnable is null");
            this.runnable = runnable;
        }

        @Override
        @DontInstrument
        public Void run() throws SuspendExecution, InterruptedException {
            runnable.run();
            return null;
        }

        public SuspendableRunnable getRunnable() {
            return runnable;
        }
    }

    public static SuspendableRunnable asSuspendable(final Runnable runnable) {
        return new SuspendableRunnable() {

            @Override
            @DontInstrument
            public void run() throws SuspendExecution, InterruptedException {
                runnable.run();
            }
        };
    }

    public static <V> SuspendableCallable<V> asSuspendable(final Callable<V> callable) {
        return new SuspendableCallable<V>() {

            @Override
            public V run() throws SuspendExecution, InterruptedException {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw Exceptions.rethrow(e);
                }
            }
        };
    }

    public static <V, E extends Exception> SuspendableCallable<V> asSuspendableCallable(final CheckedSuspendableCallable<V, E> callable) {
        return new SuspendableCallable<V>() {

            @Override
            @SuppressWarnings("UseSpecificCatch")
            public V run() throws SuspendExecution, InterruptedException {
                try {
                    return callable.call();
                } catch (final Exception e) {
                    // SuspendExecution will be handled separately by instrumentation
                    if (e instanceof InterruptedException)
                        throw (InterruptedException) e;
                    else
                        throw Exceptions.rethrow(e);
                }
            }
        };
    }
}
