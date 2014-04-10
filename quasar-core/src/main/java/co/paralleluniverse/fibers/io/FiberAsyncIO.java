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
package co.paralleluniverse.fibers.io;

import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.CompletionHandler;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
abstract class FiberAsyncIO<V> extends FiberAsync<V, IOException> {
//    private static final ThreadFactory NIO_THREAD_FACTORY = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("fiber-nio-%d").build();
//
//    public static AsynchronousChannelGroup newDefaultGroup() throws IOException {
//        return AsynchronousChannelGroup.withFixedThreadPool(1, NIO_THREAD_FACTORY);
//    }

    public static ExecutorService ioExecutor(FiberScheduler scheduler) {
        return protectScheduler(scheduler);
    }

    public static ExecutorService ioExecutor() {
        Fiber currentFiber = Fiber.currentFiber();
        if (currentFiber == null)
            throw new IllegalStateException("Method called not within a fiber");
        return ioExecutor(currentFiber.getScheduler());
    }

    public static AsynchronousChannelGroup defaultGroup() throws IOException {
        // return null; // the default group
        return AsynchronousChannelGroup.withThreadPool(ioExecutor());
    }

    public static AsynchronousChannelGroup defaultGroup(FiberScheduler scheduler) throws IOException {
        // return null; // the default group
        return AsynchronousChannelGroup.withThreadPool(ioExecutor(scheduler));
    }

    protected CompletionHandler<V, Fiber> makeCallback() {
        return new CompletionHandler<V, Fiber>() {
            @Override
            public void completed(V result, Fiber attachment) {
                FiberAsyncIO.this.asyncCompleted(result);
            }

            @Override
            public void failed(Throwable exc, Fiber attachment) {
                FiberAsyncIO.this.asyncFailed(exc);
            }
        };
    }

    @Override
    public V run() throws IOException, SuspendExecution {
        try {
            return super.run();
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    @Override
    public V run(long timeout, TimeUnit unit) throws IOException, SuspendExecution, TimeoutException {
        try {
            return super.run(timeout, unit);
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    @Suspendable
    public V runSneaky() throws IOException {
        try {
            return super.run();
        } catch (InterruptedException e) {
            throw new IOException(e);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    @Suspendable
    public static <V> V runBlockingIO(final ExecutorService exec, final CheckedCallable<V, IOException> callable) throws IOException {
        try {
            return FiberAsync.runBlocking(exec, callable);
        } catch (InterruptedException e) {
            throw new IOException(e);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    static ExecutorService protectScheduler(FiberScheduler scheduler) {
        final Executor exec = scheduler.getExecutor();
        return new ExecutorService() {

            @Override
            public void execute(Runnable command) {
                exec.execute(command);
            }

            @Override
            public void shutdown() {
            }

            @Override
            public List<Runnable> shutdownNow() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isShutdown() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isTerminated() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> Future<T> submit(Callable<T> task) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> Future<T> submit(Runnable task, T result) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Future<?> submit(Runnable task) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                throw new UnsupportedOperationException();
            }

        };
    }
}
