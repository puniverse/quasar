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

	FiberSelectSocketChannel(final SocketChannel sc) throws IOException {
		this.sc = sc;
		this.sc.configureBlocking(false);
	}

	@Suspendable
	public static FiberSelectSocketChannel open() throws IOException, SuspendExecution {
		// TODO can block the thread, make more efficient
		FiberSelectSocketChannel c = new FiberSelectSocketChannel(SocketChannel.open());
		c.register();
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

	private void connect(SocketAddress sa) throws IOException, SuspendExecution {
		sc.connect(sa);
		while (!sc.finishConnect())
			Fiber.park(key);
	}

	@Override
	public boolean isOpen() {
		return sc.isOpen();
	}

	@Override
	public void close() throws IOException {
		// TOOO if it can block run on pool
		FiberSelect.deregister(key);
		sc.close();
	}

	@Override
	@Suspendable
	public int read(final ByteBuffer dst) throws IOException {
		try {
			int count;
			while ((count = sc.read(dst)) == 0)
				Fiber.park(key);
			return count;
		} catch (SuspendExecution e) {
			throw new AssertionError(e);
		}
	}

	@Override
	@Suspendable
	public long read(final ByteBuffer[] dsts) throws IOException {
		try {
			long count;
			while ((count = sc.read(dsts)) == 0)
				Fiber.park(key);
			return count;
		} catch (SuspendExecution e) {
			throw new AssertionError(e);
		}
	}

	@Override
	@Suspendable
	public long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
		try {
			long count;
			while ((count = sc.read(dsts, offset, length)) == 0)
				Fiber.park(key);
			return count;
		} catch (SuspendExecution e) {
			throw new AssertionError(e);
		}
	}

	@Override
	@Suspendable
	public int write(final ByteBuffer src) throws IOException {
		try {
			int count;
			while ((count = sc.write(src)) == 0)
				Fiber.park(key);
			return count;
		} catch (SuspendExecution e) {
			throw new AssertionError(e);
		}
	}

	@Override
	@Suspendable
	public long write(final ByteBuffer[] srcs) throws IOException {
		try {
			long count;
			while ((count = sc.write(srcs)) == 0)
				Fiber.park(key);
			return count;
		} catch (SuspendExecution e) {
			throw new AssertionError(e);
		}
	}

	@Override
	@Suspendable
	public long write(final ByteBuffer[] srcs, int offset, int length) throws IOException {
		try {
			long count;
			while ((count = sc.write(srcs, offset, length)) == 0)
				Fiber.park(key);
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
