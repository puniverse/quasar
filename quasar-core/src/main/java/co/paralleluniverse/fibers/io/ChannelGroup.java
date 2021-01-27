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

import co.paralleluniverse.fibers.suspend.SuspendExecution;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * A grouping of channels for the purpose of resource sharing.
 *
 * <p>
 * A channel group encapsulates the mechanics required to
 * handle the completion of I/O operations initiated by channels that are bound to the group.
 *
 * A group has an associated thread pool which polls IO events sent by the OS.
 *
 * @author pron
 */
public abstract class ChannelGroup {
    /**
     * Creates a channel group with a fixed thread pool.
     *
     * <p>
     * The resulting channel group reuses a fixed number of
     * threads. At any point, at most {@code nThreads} threads will be active
     * processing tasks that are submitted to handle I/O events and dispatch
     * completion results for operations initiated on channels in
     * the group.
     *
     *
     * @param nThreads      The number of threads in the pool
     * @param threadFactory The factory to use when creating new threads
     *
     * @return A new asynchronous channel group
     *
     * @throws IllegalArgumentException If {@code nThreads <= 0}
     * @throws IOException              If an I/O error occurs
     */
    public static ChannelGroup withFixedThreadPool(int nThreads, ThreadFactory threadFactory) throws IOException {
        return new AsyncChannelGroup(AsynchronousChannelGroup.withFixedThreadPool(nThreads, threadFactory));
    }

    /**
     * Creates a channel group with a given thread pool.
     *
     * @param executor The thread pool for the resulting group
     *
     * @return A new asynchronous channel group
     */
    public static ChannelGroup withThreadPool(ExecutorService executor) throws IOException {
        return new AsyncChannelGroup(AsynchronousChannelGroup.withThreadPool(executor));
    }

    static ChannelGroup defaultGroup() throws IOException, SuspendExecution {
        return AsyncChannelGroup.getDefaultGroup();
    }

    /**
     * Shutdown the channel group.
     */
    public abstract void shutdown();

    abstract FiberSocketChannel newFiberSocketChannel() throws IOException;

    abstract FiberServerSocketChannel newFiberServerSocketChannel() throws IOException;
}
