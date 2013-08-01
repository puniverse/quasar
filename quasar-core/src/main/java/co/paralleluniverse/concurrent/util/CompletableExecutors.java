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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public final class CompletableExecutors {
    /**
     * <p>If the delegate executor was already an instance of {@code
     * CompletableExecutorService}, it is returned untouched, and the rest of this
     * documentation does not apply.
     */
    public static CompletableExecutorService completableDecorator(ExecutorService delegate) {
        return (delegate instanceof CompletableExecutorService)
                ? (CompletableExecutorService) delegate
                : (delegate instanceof ScheduledExecutorService)
                ? new ScheduledCompletableDecorator((ScheduledExecutorService) delegate)
                : new CompletableDecorator(delegate);
    }

    /**
     * <p>If the delegate executor was already an instance of {@code
     * CompletableScheduledExecutorService}, it is returned untouched, and the rest
     * of this documentation does not apply.
     */
    public static CompletableScheduledExecutorService completableDecorator(ScheduledExecutorService delegate) {
        return (delegate instanceof CompletableScheduledExecutorService)
                ? (CompletableScheduledExecutorService) delegate
                : new ScheduledCompletableDecorator(delegate);
    }

    private static class CompletableDecorator extends AbstractCompletableExecutorService {
        final ExecutorService delegate;

        CompletableDecorator(ExecutorService delegate) {
            if (delegate == null)
                throw new NullPointerException();
            this.delegate = delegate;
        }

        ExecutorService delegate() {
            return delegate;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(command);
        }
    }

    private static class ScheduledCompletableDecorator extends CompletableDecorator implements CompletableScheduledExecutorService {
        ScheduledCompletableDecorator(ScheduledExecutorService delegate) {
            super(delegate);
        }

        @Override
        ScheduledExecutorService delegate() {
            return (ScheduledExecutorService) delegate();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return delegate().schedule(command, delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return delegate().schedule(callable, delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return delegate().scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return delegate().scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }
    }

    private CompletableExecutors() {
    }
}
