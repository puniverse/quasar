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

import co.paralleluniverse.fibers.Fiber;
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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Uses an {@link java.nio.channels.Selector} to implement a fiber-blocking version of {@link SocketChannel}.
 *
 * @author circlespainter
 */
public class FiberSelectSocketChannel implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {
    private final SocketChannel sc;
    private SelectionKey key;

    public long openTime, registrationTime, connectTime, writeTime, readTime, deregistrationTime, closeTime;

    FiberSelectSocketChannel(final SocketChannel sc) throws IOException {
        this.sc = sc;
        this.sc.configureBlocking(false);
    }

    @Suspendable
    public static FiberSelectSocketChannel open() throws IOException, SuspendExecution {
        // TODO can block the thread, make more efficient
        long start = System.nanoTime();
        FiberSelectSocketChannel c = new FiberSelectSocketChannel(SocketChannel.open());
        long end = System.nanoTime();
        c.openTime = end - start;

        start = System.nanoTime();
        c.register();
        end = System.nanoTime();
        c.registrationTime = end - start;
        return c;
    }

    public static FiberSelectSocketChannel open(final SocketAddress sa) throws IOException, SuspendExecution {
        final FiberSelectSocketChannel res = open();
        res.connect(sa);
        return res;
    }

    private void register() throws IOException, SuspendExecution {
        this.key = FiberSelect.register(sc);
    }

    public void connect(SocketAddress sa) throws IOException, SuspendExecution {
        long start = System.nanoTime();
        sc.connect(sa);
        while (!sc.finishConnect())
            Fiber.park(key);
        long end = System.nanoTime();
        connectTime = end - start;
    }

    @Override
    public boolean isOpen() {
        return sc.isOpen();
    }

    @Override
    public void close() throws IOException {
        long start = System.nanoTime();
        // TOOO if it can block run on pool
        FiberSelect.deregister(key);
        long end = System.nanoTime();
        deregistrationTime = end - start;
        start = System.nanoTime();
        sc.close();
        end = System.nanoTime();
        closeTime = end - start;
    }

    @Override
    @Suspendable
    public int read(final ByteBuffer dst) throws IOException {
        long start = System.nanoTime();
        try {
            int count;
            while ((count = sc.read(dst)) == 0)
                Fiber.park(key);
            long end = System.nanoTime();
            readTime += end - start;
            return count;
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @Suspendable
    public long read(final ByteBuffer[] dsts) throws IOException {
        long start = System.nanoTime();
        try {
            long count;
            while ((count = sc.read(dsts)) == 0)
                Fiber.park(key);
            long end = System.nanoTime();
            readTime += end - start;
            return count;
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @Suspendable
    public long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
        long start = System.nanoTime();
        try {
            long count;
            while ((count = sc.read(dsts, offset, length)) == 0)
                Fiber.park(key);
            long end = System.nanoTime();
            readTime += end - start;
            return count;
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @Suspendable
    public int write(final ByteBuffer src) throws IOException {
        long start = System.nanoTime();
        try {
            int count;
            while ((count = sc.write(src)) == 0)
                Fiber.park(key);
            long end = System.nanoTime();
            writeTime += end - start;
            return count;
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @Suspendable
    public long write(final ByteBuffer[] srcs) throws IOException {
        long start = System.nanoTime();
        try {
            long count;
            while ((count = sc.write(srcs)) == 0)
                Fiber.park(key);
            long end = System.nanoTime();
            writeTime += end - start;
            return count;
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @Suspendable
    public long write(final ByteBuffer[] srcs, int offset, int length) throws IOException {
        long start = System.nanoTime();
        try {
            long count;
            while ((count = sc.write(srcs, offset, length)) == 0)
                Fiber.park(key);
            long end = System.nanoTime();
            writeTime += end - start;
            return count;
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
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
