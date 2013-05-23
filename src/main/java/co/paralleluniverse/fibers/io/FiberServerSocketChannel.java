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
package co.paralleluniverse.fibers.io;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NetworkChannel;
import java.util.Set;

/**
 *
 * @author pron
 */
public class FiberServerSocketChannel implements NetworkChannel {
    private final AsynchronousServerSocketChannel ac;

    private FiberServerSocketChannel(AsynchronousServerSocketChannel assc) {
        this.ac = assc;
    }

    public static FiberServerSocketChannel open() throws IOException {
        return new FiberServerSocketChannel(AsynchronousServerSocketChannel.open());
    }

    public static FiberServerSocketChannel open(AsynchronousChannelGroup group) throws IOException {
        return new FiberServerSocketChannel(AsynchronousServerSocketChannel.open(group));
    }

    public FiberSocketChannel accept() throws IOException, SuspendExecution {
        return new FiberSocketChannel(new FiberAsyncIO<AsynchronousSocketChannel>() {
            @Override
            protected void requestAsync(Fiber current, CompletionHandler<AsynchronousSocketChannel, Fiber> completionHandler) {
                ac.accept(current, completionHandler);
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
    public FiberServerSocketChannel bind(SocketAddress local) throws IOException {
        ac.bind(local);
        return this;
    }

    public FiberServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        ac.bind(local, backlog);
        return this;
    }

    @Override
    public <T> FiberServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
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
