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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Uses an {@link AsynchronousSocketChannel} to implement a fiber-blocking version of {@link SocketChannel}.
 *
 * @author pron
 */
public class FiberSocketChannel implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {
    private final AsynchronousSocketChannel ac;

    public FiberSocketChannel(AsynchronousSocketChannel asc) {
        this.ac = asc;
    }

    /**
     * Opens a socket channel. 
     * Same as {@link #open(java.nio.channels.AsynchronousChannelGroup) open((AsynchronousChannelGroup) null)}.
     *
     * @return A new socket channel
     * @throws IOException If an I/O error occurs
     */
    public static FiberSocketChannel open() throws IOException {
        return new FiberSocketChannel(AsynchronousSocketChannel.open(FiberAsyncIO.newDefaultGroup()));
    }

    /**
     * Opens a socket channel.
     *
     * <p> The new channel is created by invoking the {@link
     * AsynchronousChannelProvider#openAsynchronousSocketChannel
     * openAsynchronousSocketChannel} method on the {@link
     * AsynchronousChannelProvider} that created the group. If the group parameter
     * is {@code null} then the resulting channel is created by the system-wide
     * default provider, and bound to the <em>default group</em>.
     *
     * @param group The group to which the newly constructed channel should be bound, or {@code null} for the default group
     * @return A new socket channel
     * @throws ShutdownChannelGroupException If the channel group is shutdown
     * @throws IOException                   If an I/O error occurs
     */
    public static FiberSocketChannel open(AsynchronousChannelGroup group) throws IOException {
        return new FiberSocketChannel(AsynchronousSocketChannel.open(group));
    }

    /**
     * Opens a socket channel and connects it to a remote address.
     *
     * <p> This convenience method works as if by invoking the {@link #open()}
     * method, invoking the {@link #connect(SocketAddress) connect} method upon
     * the resulting socket channel, passing it <tt>remote</tt>, and then
     * returning that channel. </p>
     *
     * @param remote The remote address to which the new channel is to be connected
     *
     * @throws AsynchronousCloseException      If another thread closes this channel
     *                                         while the connect operation is in progress
     * @throws ClosedByInterruptException      If another thread interrupts the current thread
     *                                         while the connect operation is in progress, thereby
     *                                         closing the channel and setting the current thread's
     *                                         interrupt status
     * @throws UnresolvedAddressException      If the given remote address is not fully resolved
     * @throws UnsupportedAddressTypeException If the type of the given remote address is not supported
     * @throws SecurityException               If a security manager has been installed
     *                                         and it does not permit access to the given remote endpoint
     * @throws IOException                     If some other I/O error occurs
     */
    public static FiberSocketChannel open(SocketAddress remote) throws IOException, SuspendExecution {
        final FiberSocketChannel channel = open();
        channel.connect(remote);
        return channel;
    }

    /**
     * Opens a socket channel and connects it to a remote address.
     *
     * <p> This convenience method works as if by invoking the {@link #open()}
     * method, invoking the {@link #connect(SocketAddress) connect} method upon
     * the resulting socket channel, passing it <tt>remote</tt>, and then
     * returning that channel. </p>
     *
     * @param group  The group to which the newly constructed channel should be bound, or {@code null} for the default group
     * @param remote The remote address to which the new channel is to be connected
     *
     * @throws AsynchronousCloseException      If another thread closes this channel
     *                                         while the connect operation is in progress
     * @throws ClosedByInterruptException      If another thread interrupts the current thread
     *                                         while the connect operation is in progress, thereby
     *                                         closing the channel and setting the current thread's
     *                                         interrupt status
     * @throws UnresolvedAddressException      If the given remote address is not fully resolved
     * @throws UnsupportedAddressTypeException If the type of the given remote address is not supported
     * @throws SecurityException               If a security manager has been installed
     *                                         and it does not permit access to the given remote endpoint
     * @throws IOException                     If some other I/O error occurs
     */
    public static FiberSocketChannel open(AsynchronousChannelGroup group, SocketAddress remote) throws IOException, SuspendExecution {
        final FiberSocketChannel channel = open(group);
        channel.connect(remote);
        return channel;
    }

    /**
     * Connects this channel.
     *
     * <p> This method initiates an operation to connect this channel. it blocks
     * until the connection is successfully established or connection cannot be
     * established. If the connection cannot be established then the channel is
     * closed.
     *
     * <p> This method performs exactly the same security checks as the {@link
     * java.net.Socket} class. That is, if a security manager has been
     * installed then this method verifies that its {@link
     * java.lang.SecurityManager#checkConnect checkConnect} method permits
     * connecting to the address and port number of the given remote endpoint.
     *
     * @param remote The remote address to which this channel is to be connected
     *
     * @throws UnresolvedAddressException      If the given remote address is not fully resolved
     * @throws UnsupportedAddressTypeException If the type of the given remote address is not supported
     * @throws AlreadyConnectedException       If this channel is already connected
     * @throws ConnectionPendingException      If a connection operation is already in progress on this channel
     * @throws ShutdownChannelGroupException   If the channel group has terminated
     * @throws SecurityException               If a security manager has been installed and it does not permit access to the given remote endpoint
     *
     * @see #getRemoteAddress
     */
    public void connect(final SocketAddress remote) throws IOException, SuspendExecution {
        new FiberAsyncIO<Void>() {
            @Override
            protected Void requestAsync() {
                ac.connect(remote, null, makeCallback());
                return null;
            }
        }.run();
    }

    /**
     * Reads a sequence of bytes from this channel into a subsequence of the
     * given buffers. This operation, sometimes called a <em>scattering read</em>,
     * is often useful when implementing network protocols that group data into
     * segments consisting of one or more fixed-length headers followed by a
     * variable-length body. This method blocks until the read operation completes or fails.
     *
     * The returned result is the number of bytes read or
     * {@code -1} if no bytes could be read because the channel has reached
     * end-of-stream.
     *
     * <p> This method initiates a read of up to <i>r</i> bytes from this channel,
     * where <i>r</i> is the total number of bytes remaining in the specified
     * subsequence of the given buffer array, that is,
     *
     * <blockquote><pre>
     * dsts[offset].remaining()
     *     + dsts[offset+1].remaining()
     *     + ... + dsts[offset+length-1].remaining()</pre></blockquote>
     *
     * at the moment that the read is attempted.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is read, where
     * <tt>0</tt>&nbsp;<tt>&lt;</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;<i>r</i>.
     * Up to the first <tt>dsts[offset].remaining()</tt> bytes of this sequence
     * are transferred into buffer <tt>dsts[offset]</tt>, up to the next
     * <tt>dsts[offset+1].remaining()</tt> bytes are transferred into buffer
     * <tt>dsts[offset+1]</tt>, and so forth, until the entire byte sequence
     * is transferred into the given buffers. As many bytes as possible are
     * transferred into each buffer, hence the final position of each updated
     * buffer, except the last updated buffer, is guaranteed to be equal to
     * that buffer's limit. The underlying operating system may impose a limit
     * on the number of buffers that may be used in an I/O operation. Where the
     * number of buffers (with bytes remaining), exceeds this limit, then the
     * I/O operation is performed with the maximum number of buffers allowed by
     * the operating system.
     *
     * <p> If a timeout is specified and the timeout elapses before the operation
     * completes then the method throws exception {@link
     * InterruptedByTimeoutException}. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been read, or will not
     * be read from the channel into the given buffers, then further attempts to
     * read from the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * @param dsts    The buffers into which bytes are to be transferred
     * @param offset  The offset within the buffer array of the first buffer into which
     *                bytes are to be transferred; must be non-negative and no larger than
     *                {@code dsts.length}
     * @param length  The maximum number of buffers to be accessed; must be non-negative
     *                and no larger than {@code dsts.length - offset}
     * @param timeout The maximum time for the I/O operation to complete
     * @param unit    The time unit of the {@code timeout} argument
     * @return the number of bytes read or {@code -1} if no bytes could be read because the channel has reached end-of-stream.
     *
     * @throws IndexOutOfBoundsException     If the pre-conditions for the {@code offset} and {@code length} parameter aren't met
     * @throws IllegalArgumentException      If the buffer is read-only
     * @throws ReadPendingException          If a read operation is already in progress on this channel
     * @throws NotYetConnectedException      If this channel is not yet connected
     * @throws ShutdownChannelGroupException If the channel group has terminated
     * @throws InterruptedByTimeoutException If a timeout is specified and the timeout elapses before the operation completes
     */
    public long read(final ByteBuffer[] dsts, final int offset, final int length, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new FiberAsyncIO<Long>() {
            @Override
            protected Void requestAsync() {
                ac.read(dsts, offset, length, timeout, unit, null, makeCallback());
                return null;
            }
        }.run();
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p> This method reads a sequence of bytes from this channel into the given buffer.
     *
     * The returned result is the number of bytes read or
     * {@code -1} if no bytes could be read because the channel has reached
     * end-of-stream.
     *
     * <p> If a timeout is specified and the timeout elapses before the method throws {@link
     * InterruptedByTimeoutException}. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been read, or will not
     * be read from the channel into the given buffer, then further attempts to
     * read from the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * <p> Otherwise this method works in the same manner as the {@link #read(ByteBuffer)}.
     * method.
     *
     * @param dst     The buffer into which bytes are to be transferred
     * @param timeout The maximum time for the I/O operation to complete
     * @param unit    The time unit of the {@code timeout} argument
     * @return the number of bytes read or {@code -1} if no bytes could be read because the channel has reached end-of-stream.
     *
     * @throws IllegalArgumentException      If the buffer is read-only
     * @throws ReadPendingException          If a read operation is already in progress on this channel
     * @throws NotYetConnectedException      If this channel is not yet connected
     * @throws ShutdownChannelGroupException If the channel group has terminated
     * @throws InterruptedByTimeoutException If a timeout is specified and the timeout elapses before the operation completes
     */
    public int read(final ByteBuffer dst, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new FiberAsyncIO<Integer>() {
            @Override
            protected Void requestAsync() {
                ac.read(dst, timeout, unit, null, makeCallback());
                return null;
            }
        }.run();
    }

    /**
     * Writes a sequence of bytes to this channel from a subsequence of the given
     * buffers. This operation, sometimes called a <em>gathering write</em>, is
     * often useful when implementing network protocols that group data into
     * segments consisting of one or more fixed-length headers followed by a
     * variable-length body.
     * 
     * The returned result is the number of bytes written.
     * 
     * <p> This method writes of up to <i>r</i> bytes to this channel,
     * where <i>r</i> is the total number of bytes remaining in the specified
     * subsequence of the given buffer array, that is,
     *
     * <blockquote><pre>
     * srcs[offset].remaining()
     *     + srcs[offset+1].remaining()
     *     + ... + srcs[offset+length-1].remaining()</pre></blockquote>
     *
     * at the moment that the write is attempted.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is written, where
     * <tt>0</tt>&nbsp;<tt>&lt;</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;<i>r</i>.
     * Up to the first <tt>srcs[offset].remaining()</tt> bytes of this sequence
     * are written from buffer <tt>srcs[offset]</tt>, up to the next
     * <tt>srcs[offset+1].remaining()</tt> bytes are written from buffer
     * <tt>srcs[offset+1]</tt>, and so forth, until the entire byte sequence is
     * written. As many bytes as possible are written from each buffer, hence
     * the final position of each updated buffer, except the last updated
     * buffer, is guaranteed to be equal to that buffer's limit. The underlying
     * operating system may impose a limit on the number of buffers that may be
     * used in an I/O operation. Where the number of buffers (with bytes
     * remaining), exceeds this limit, then the I/O operation is performed with
     * the maximum number of buffers allowed by the operating system.
     *
     * <p> If a timeout is specified and the timeout elapses before the operation
     * completes then the method throws the exception {@link
     * InterruptedByTimeoutException}. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been written, or will
     * not be written to the channel from the given buffers, then further attempts
     * to write to the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * @param srcs    The buffers from which bytes are to be retrieved
     * @param offset  The offset within the buffer array of the first buffer from which
     *                bytes are to be retrieved; must be non-negative and no larger than {@code srcs.length}
     * @param length  The maximum number of buffers to be accessed; must be non-negative and no larger than {@code srcs.length - offset}
     * @param timeout The maximum time for the I/O operation to complete
     * @param unit    The time unit of the {@code timeout} argument
     *
     * @throws IndexOutOfBoundsException     If the pre-conditions for the {@code offset} and {@code length} parameter aren't met
     * @throws WritePendingException         If a write operation is already in progress on this channel
     * @throws NotYetConnectedException      If this channel is not yet connected
     * @throws ShutdownChannelGroupException If the channel group has terminated
     * @throws InterruptedByTimeoutException If a timeout is specified and the timeout elapses before the operation completes
     */
    public long write(final ByteBuffer[] srcs, final int offset, final int length, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new FiberAsyncIO<Long>() {
            @Override
            protected Void requestAsync() {
                ac.write(srcs, offset, length, timeout, unit, null, makeCallback());
                return null;
            }
        }.run();
    }

    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * <p> This writes a
     * sequence of bytes to this channel from the given buffer.
     * 
     * The returned result is the number of bytes written.
     *
     * <p> If a timeout is specified and the timeout elapses before the operation
     * completes then the method throws the exception {@link
     * InterruptedByTimeoutException}. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been written, or will
     * not be written to the channel from the given buffer, then further attempts
     * to write to the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * <p> Otherwise this method works in the same manner as the {@link #write(ByteBuffer)} method.
     *
     * @param src     The buffer from which bytes are to be retrieved
     * @param timeout The maximum time for the I/O operation to complete
     * @param unit    The time unit of the {@code timeout} argument
     *
     * @throws WritePendingException         If a write operation is already in progress on this channel
     * @throws NotYetConnectedException      If this channel is not yet connected
     * @throws ShutdownChannelGroupException If the channel group has terminated
     * @throws InterruptedByTimeoutException If a timeout is specified and the timeout elapses before the operation completes
     */
    public int write(final ByteBuffer src, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new FiberAsyncIO<Integer>() {
            @Override
            protected Void requestAsync() {
                ac.write(src, timeout, unit, null, makeCallback());
                return null;
            }
        }.run();
    }

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        try {
            return read(dsts, offset, length, 0L, TimeUnit.MILLISECONDS);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public int read(ByteBuffer dst) throws IOException {
        try {
            return read(dst, 0L, TimeUnit.MILLISECONDS);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        try {
            return write(srcs, offset, length, 0L, TimeUnit.MILLISECONDS);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
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

    /**
     * Shutdown the connection for reading without closing the channel.
     *
     * <p> Once shutdown for reading then further reads on the channel will
     * return {@code -1}, the end-of-stream indication. If the input side of the
     * connection is already shutdown then invoking this method has no effect.
     * The effect on an outstanding read operation is system dependent and
     * therefore not specified. The effect, if any, when there is data in the
     * socket receive buffer that has not been read, or data arrives subsequently,
     * is also system dependent.
     *
     * @return The channel
     * @throws NotYetConnectedException If this channel is not yet connected
     * @throws ClosedChannelException   If this channel is closed
     * @throws IOException              If some other I/O error occurs
     */
    public FiberSocketChannel shutdownInput() throws IOException {
        ac.shutdownInput();
        return this;
    }

    /**
     * Shutdown the connection for writing without closing the channel.
     *
     * <p> Once shutdown for writing then further attempts to write to the
     * channel will throw {@link ClosedChannelException}. If the output side of
     * the connection is already shutdown then invoking this method has no
     * effect. The effect on an outstanding write operation is system dependent
     * and therefore not specified.
     *
     * @return The channel
     * @throws NotYetConnectedException If this channel is not yet connected
     * @throws ClosedChannelException   If this channel is closed
     * @throws IOException              If some other I/O error occurs
     */
    public FiberSocketChannel shutdownOutput() throws IOException {
        ac.shutdownOutput();
        return this;
    }

    /**
     * Returns the remote address to which this channel's socket is connected.
     *
     * <p> Where the channel is bound and connected to an Internet Protocol
     * socket address then the return value from this method is of type {@link
     * java.net.InetSocketAddress}.
     *
     * @return The remote address; {@code null} if the channel's socket is not connected
     *
     * @throws ClosedChannelException If the channel is closed
     * @throws IOException            If an I/O error occurs
     */
    public SocketAddress getRemoteAddress() throws IOException {
        return ac.getRemoteAddress();
    }

    /**
     * Returns the provider that created this channel.
     */
    public final AsynchronousChannelProvider provider() {
        return ac.provider();
    }

    /**
     * @throws ConnectionPendingException
     *                                         If a connection operation is already in progress on this channel
     * @throws AlreadyBoundException           {@inheritDoc}
     * @throws UnsupportedAddressTypeException {@inheritDoc}
     * @throws ClosedChannelException          {@inheritDoc}
     * @throws IOException                     {@inheritDoc}
     */
    @Override
    public FiberSocketChannel bind(SocketAddress local) throws IOException {
        ac.bind(local);
        return this;
    }

    /**
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ClosedChannelException   {@inheritDoc}
     * @throws IOException              {@inheritDoc}
     */
    @Override
    public <T> FiberSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
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
