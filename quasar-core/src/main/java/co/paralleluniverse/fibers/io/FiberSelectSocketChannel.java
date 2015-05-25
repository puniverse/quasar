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
        return res;
    }

    @Override
    public boolean isOpen() {
        return sc.isOpen();
    }

    @Override
    public void close() throws IOException {
        // TODO can block the thread, make more efficient
        sc.close();
    }

    @Override
    @Suspendable
    public int read(final ByteBuffer dst) throws IOException {
        FiberSelect.forRead(sc);
        return sc.read(dst);
    }

    @Override
    @Suspendable
    public long read(final ByteBuffer[] dsts) throws IOException {
        FiberSelect.forRead(sc);
        return sc.read(dsts);
    }

    @Override
    @Suspendable
    public long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
        FiberSelect.forRead(sc);
        return sc.read(dsts, offset, length);
    }

    @Override
    @Suspendable
    public int write(final ByteBuffer src) throws IOException {
        FiberSelect.forWrite(sc);
        return sc.write(src);
    }

    @Override
    @Suspendable
    public long write(final ByteBuffer[] srcs) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    @Suspendable
    public long write(final ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NetworkChannel bind(final SocketAddress local) throws IOException {
        // TODO can this block the thread? Make it more efficient if it can
        sc.bind(local);
        return this;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        // TODO can this block the thread? Make it more efficient if it can
        return sc.getLocalAddress();
    }

    @Override
    public <T> NetworkChannel setOption(final SocketOption<T> name, final T value) throws IOException {
        // TODO can this block the thread? Make it more efficient if it can
        sc.setOption(name, value);
        return this;
    }

    @Override
    public <T> T getOption(final SocketOption<T> name) throws IOException {
        // TODO can this block the thread? Make it more efficient if it can
        return sc.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return sc.supportedOptions();
    }
}
