/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SchedulerLocal;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
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
 * Uses an {@link AsynchronousChannelGroup} to implement a {@link ChannelGroup}.
 *
 * @author pron
 */
final class AsyncChannelGroup extends ChannelGroup {
    private final AsynchronousChannelGroup group;

    AsyncChannelGroup(AsynchronousChannelGroup group) {
        this.group = group;
    }

    @Override
    FiberSocketChannel newFiberSocketChannel() throws IOException {
        return new AsyncFiberSocketChannel(AsynchronousSocketChannel.open(group));
    }

    @Override
    FiberServerSocketChannel newFiberServerSocketChannel() throws IOException {
        return new AsyncFiberServerSocketChannel(AsynchronousServerSocketChannel.open(group));
    }

    @Override
    public void shutdown() {
        group.shutdown();
    }

    private static final SchedulerLocal<AsyncChannelGroup> defaultGroup = new SchedulerLocal<AsyncChannelGroup>() {

        @Override
        protected AsyncChannelGroup initialValue(FiberScheduler scheduler) {
            try {
                return new AsyncChannelGroup(AsynchronousChannelGroup.withThreadPool(protectScheduler(scheduler)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

//    private static final ThreadFactory NIO_THREAD_FACTORY = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("fiber-nio-%d").build();
//
//    public static AsynchronousChannelGroup newDefaultGroup() throws IOException {
//        return AsynchronousChannelGroup.withFixedThreadPool(1, NIO_THREAD_FACTORY);
//    }
    static AsyncChannelGroup getDefaultGroup() throws IOException, SuspendExecution {
        return defaultGroup.get();
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
