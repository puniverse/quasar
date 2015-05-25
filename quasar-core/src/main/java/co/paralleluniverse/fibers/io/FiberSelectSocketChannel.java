/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Uses an {@link java.nio.channels.Selector} to implement a fiber-blocking version of {@link SocketChannel}.
 *
 * @author circlespainter
 */
public class FiberSelectSocketChannel implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {
    private final SocketChannel sc;

    FiberSelectSocketChannel(final SocketChannel sc) throws IOException {
        this.sc = sc;
        this.sc.configureBlocking(false);
    }

    private void startConnect(final SocketAddress sa) throws IOException {
        sc.connect(sa);
    }
    
    public static FiberSelectSocketChannel open() throws IOException {
        // TODO can block the thread, make more efficient
        return new FiberSelectSocketChannel(SocketChannel.open());
    }

    public static FiberSelectSocketChannel open(final SocketAddress sa) throws IOException, SuspendExecution {
        final FiberSelectSocketChannel res = open();
        res.startConnect(sa);
        FiberSelect.forConnect(res.sc);
        if (res.sc.isConnectionPending())
            // TOOO if it can block run on pool
            res.sc.finishConnect();
        return res;
    }

    @Override
    public boolean isOpen() {
        return sc.isOpen();
    }

    @Override
    public void close() throws IOException {
        // TOOO if it can block run on pool
        sc.close();
    }

    @Override
    @Suspendable
    public int read(final ByteBuffer dst) throws IOException {
        int count = sc.read(dst);
        if (count == 0) {
            FiberSelect.forRead(sc);
            return sc.read(dst);
        } else
            return count;
    }

    @Override
    @Suspendable
    public long read(final ByteBuffer[] dsts) throws IOException {
        long count = sc.read(dsts);
        if (count == 0) {
            FiberSelect.forRead(sc);
            return sc.read(dsts);
        } else
            return count;
    }

    @Override
    @Suspendable
    public long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
        long count = sc.read(dsts, offset, length);
        if (count == 0) {
            FiberSelect.forRead(sc);
            return sc.read(dsts, offset, length);
        } else
            return count;
    }

    @Override
    @Suspendable
    public int write(final ByteBuffer src) throws IOException {
        int count = sc.write(src);
        if (count == 0) {
            FiberSelect.forWrite(sc);
            return sc.write(src);
        } else
            return count;
    }

    @Override
    @Suspendable
    public long write(final ByteBuffer[] srcs) throws IOException {
        long count = sc.write(srcs);
        if (count == 0) {
            FiberSelect.forWrite(sc);
            return sc.write(srcs);
        } else
            return count;
    }

    @Override
    @Suspendable
    public long write(final ByteBuffer[] srcs, int offset, int length) throws IOException {
        long count = sc.write(srcs, offset, length);
        if (count == 0) {
            FiberSelect.forWrite(sc);
            return sc.write(srcs, offset, length);
        } else
            return count;
    }

    @Override
    public NetworkChannel bind(final SocketAddress local) throws IOException {
        // TOOO if it can block run on pool
        sc.bind(local);
        return this;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        // TOOO if it can block run on pool
        return sc.getLocalAddress();
    }

    @Override
    public <T> NetworkChannel setOption(final SocketOption<T> name, final T value) throws IOException {
        // TOOO if it can block run on pool
        sc.setOption(name, value);
        return this;
    }

    @Override
    public <T> T getOption(final SocketOption<T> name) throws IOException {
        // TOOO if it can block run on pool
        return sc.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return sc.supportedOptions();
    }
}
