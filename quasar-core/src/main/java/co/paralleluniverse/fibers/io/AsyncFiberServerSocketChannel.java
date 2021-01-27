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
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.Set;

/**
 * Uses an {@link AsynchronousServerSocketChannel} to implement a fiber-blocking version of {@link ServerSocketChannel}.
 *
 * @author pron
 */
final class AsyncFiberServerSocketChannel extends FiberServerSocketChannel implements NetworkChannel {
    private final AsynchronousServerSocketChannel ac;

    AsyncFiberServerSocketChannel(AsynchronousServerSocketChannel assc) {
        this.ac = assc;
    }

    @Override
    public final AsynchronousChannelProvider provider() {
        return ac.provider();
    }

    @Override
    public FiberSocketChannel accept() throws IOException, SuspendExecution {
        return new AsyncFiberSocketChannel(new FiberAsyncIO<AsynchronousSocketChannel>() {
            @Override
            protected void requestAsync() {
                ac.accept(null, makeCallback());
            }
        }.run());
    }

    @Override
    public boolean isOpen() {
        return ac.isOpen();
    }

    @Override
    public void close() throws IOException {
        ac.close();
    }

    @Override
    public AsyncFiberServerSocketChannel bind(SocketAddress local) throws IOException {
        ac.bind(local);
        return this;
    }

    @Override
    public AsyncFiberServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        ac.bind(local, backlog);
        return this;
    }

    @Override
    public <T> AsyncFiberServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        ac.setOption(name, value);
        return this;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return ac.getLocalAddress();
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return ac.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return ac.supportedOptions();
    }
}
