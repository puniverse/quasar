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

import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Uses an {@link AsynchronousSocketChannel} to implement a fiber-blocking version of {@link SocketChannel}.
 *
 * @author pron
 */
final class AsyncFiberSocketChannel extends FiberSocketChannel implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {
    private final AsynchronousSocketChannel ac;

    AsyncFiberSocketChannel(AsynchronousSocketChannel asc) {
        this.ac = asc;
    }

    @Override
    public final AsynchronousChannelProvider provider() {
        return ac.provider();
    }

    @Override
    public void connect(final SocketAddress remote) throws IOException, SuspendExecution {
        new FiberAsyncIO<Void>() {
            @Override
            protected void requestAsync() {
                ac.connect(remote, null, makeCallback());
            }
        }.run();
    }

    @Override
    public void connect(final SocketAddress remote, final long timeout, final TimeUnit timeUnit) throws IOException, SuspendExecution, TimeoutException {
        new FiberAsyncIO<Void>() {
            @Override
            protected void requestAsync() {
                ac.connect(remote, null, makeCallback());
            }
        }.run(timeout, timeUnit);
    }

    @Override
    public long read(final ByteBuffer[] dsts, final int offset, final int length, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new FiberAsyncIO<Long>() {
            @Override
            protected void requestAsync() {
                ac.read(dsts, offset, length, timeout, unit, null, makeCallback());
            }
        }.run();
    }

    @Override
    public int read(final ByteBuffer dst, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new FiberAsyncIO<Integer>() {
            @Override
            protected void requestAsync() {
                ac.read(dst, timeout, unit, null, makeCallback());
            }
        }.run();
    }

    @Override
    public long write(final ByteBuffer[] srcs, final int offset, final int length, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new FiberAsyncIO<Long>() {
            @Override
            protected void requestAsync() {
                ac.write(srcs, offset, length, timeout, unit, null, makeCallback());
            }
        }.run();
    }

    @Override
    public int write(final ByteBuffer src, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new FiberAsyncIO<Integer>() {
            @Override
            protected void requestAsync() {
                ac.write(src, timeout, unit, null, makeCallback());
            }
        }.run();
    }

    @Override
    @Suspendable
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        try {
            return read(dsts, offset, length, 0L, TimeUnit.MILLISECONDS);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    @Override
    @Suspendable
    public long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    @Override
    @Suspendable
    public int read(ByteBuffer dst) throws IOException {
        try {
            return read(dst, 0L, TimeUnit.MILLISECONDS);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    @Override
    @Suspendable
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        try {
            return write(srcs, offset, length, 0L, TimeUnit.MILLISECONDS);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    @Override
    @Suspendable
    public long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }

    @Override
    @Suspendable
    public int write(final ByteBuffer src) throws IOException {
        try {
            return write(src, 0L, TimeUnit.MILLISECONDS);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
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
    public AsyncFiberSocketChannel shutdownInput() throws IOException {
        ac.shutdownInput();
        return this;
    }

    @Override
    public AsyncFiberSocketChannel shutdownOutput() throws IOException {
        ac.shutdownOutput();
        return this;
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return ac.getRemoteAddress();
    }

    @Override
    public AsyncFiberSocketChannel bind(SocketAddress local) throws IOException {
        ac.bind(local);
        return this;
    }

    @Override
    public <T> AsyncFiberSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
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

    private int remotePort() throws IOException {
        return ((java.net.InetSocketAddress) ac.getRemoteAddress()).getPort();
    }
}
