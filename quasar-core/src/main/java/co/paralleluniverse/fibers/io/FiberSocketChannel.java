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
import java.nio.channels.*;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A fiber-blocking version of {@link SocketChannel}.
 *
 * @author pron
 */
public abstract class FiberSocketChannel implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {
    /**
     * Opens a socket channel.
     *
     * <p>
     * If the group parameter is {@code null} then the resulting channel is bound to the <em>default group</em>.
     *
     * @param group The group to which the newly constructed channel should be bound, or {@code null} for the default group
     * @return A new socket channel
     * @throws ShutdownChannelGroupException If the channel group is shutdown
     * @throws IOException                   If an I/O error occurs
     */
    public static FiberSocketChannel open(ChannelGroup group) throws IOException, SuspendExecution {
        if (group == null)
            group = ChannelGroup.defaultGroup();
        return group.newFiberSocketChannel();
    }

    /**
     * Opens a socket channel.
     * Same as {@link #open(ChannelGroup) open((AsynchronousChannelGroup) null)}.
     *
     * @return A new socket channel
     * @throws IOException If an I/O error occurs
     */
    public static FiberSocketChannel open() throws IOException, SuspendExecution {
        return open((ChannelGroup) null);
    }

    /**
     * Opens a socket channel and connects it to a remote address.
     *
     * <p>
     * This convenience method works as if by invoking the {@link #open()}
     * method, invoking the {@link #connect(SocketAddress) connect} method upon
     * the resulting socket channel, passing it {@code remote}, and then
     * returning that channel. </p>
     *
     * @param remote The remote address to which the new channel is to be connected
     * @return A new socket channel
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
     * <p>
     * This convenience method works as if by invoking the {@link #open()}
     * method, invoking the {@link #connect(SocketAddress) connect} method upon
     * the resulting socket channel, passing it {@code remote}, and then
     * returning that channel. </p>
     *
     * @param group  The group to which the newly constructed channel should be bound, or {@code null} for the default group
     * @param remote The remote address to which the new channel is to be connected
     * @return A new socket channel
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
    public static FiberSocketChannel open(ChannelGroup group, SocketAddress remote) throws IOException, SuspendExecution {
        final FiberSocketChannel channel = open(group);
        channel.connect(remote);
        return channel;
    }

    /**
     * Connects this channel.
     *
     * <p>
     * This method initiates an operation to connect this channel. it blocks
     * until the connection is successfully established or connection cannot be
     * established. If the connection cannot be established then the channel is
     * closed.
     *
     * <p>
     * This method performs exactly the same security checks as the {@link
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
    public abstract void connect(final SocketAddress remote) throws IOException, SuspendExecution;

    /**
     * Connects this channel.
     *
     * <p>
     * This method initiates an operation to connect this channel. it blocks
     * until the connection is successfully established or connection cannot be
     * established or a timeout occurs while attempting to establish it. If the
     * connection cannot be established then the channel is closed but not so if
     * a timeout occurs.
     *
     * <p>
     * This method performs exactly the same security checks as the {@link
     * java.net.Socket} class. That is, if a security manager has been
     * installed then this method verifies that its {@link
     * java.lang.SecurityManager#checkConnect checkConnect} method permits
     * connecting to the address and port number of the given remote endpoint.
     *
     * @param remote   The remote address to which this channel is to be connected
     * @param timeout  The timeout for the connection attempt
     * @param timeUnit The time unit for the connection attempt timeout
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
    public abstract void connect(final SocketAddress remote, final long timeout, final TimeUnit timeUnit) throws IOException, SuspendExecution, TimeoutException;

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
     * <p>
     * This method initiates a read of up to <i>r</i> bytes from this channel,
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
     * <p>
     * Suppose that a byte sequence of length <i>n</i> is read, where
     * {@code 0}&nbsp;{@code &lt;}&nbsp;<i>n</i>&nbsp;{@code &lt;=}&nbsp;<i>r</i>.
     * Up to the first {@code dsts[offset].remaining()} bytes of this sequence
     * are transferred into buffer {@code dsts[offset]}, up to the next
     * {@code dsts[offset+1].remaining()} bytes are transferred into buffer
     * {@code dsts[offset+1]}, and so forth, until the entire byte sequence
     * is transferred into the given buffers. As many bytes as possible are
     * transferred into each buffer, hence the final position of each updated
     * buffer, except the last updated buffer, is guaranteed to be equal to
     * that buffer's limit. The underlying operating system may impose a limit
     * on the number of buffers that may be used in an I/O operation. Where the
     * number of buffers (with bytes remaining), exceeds this limit, then the
     * I/O operation is performed with the maximum number of buffers allowed by
     * the operating system.
     *
     * <p>
     * If a timeout is specified and the timeout elapses before the operation
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
    public abstract long read(final ByteBuffer[] dsts, final int offset, final int length, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution;

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p>
     * This method reads a sequence of bytes from this channel into the given buffer.
     *
     * The returned result is the number of bytes read or
     * {@code -1} if no bytes could be read because the channel has reached
     * end-of-stream.
     *
     * <p>
     * If a timeout is specified and the timeout elapses before the method throws {@link
     * InterruptedByTimeoutException}. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been read, or will not
     * be read from the channel into the given buffer, then further attempts to
     * read from the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * <p>
     * Otherwise this method works in the same manner as the {@link #read(ByteBuffer)}.
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
    public abstract int read(final ByteBuffer dst, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution;

    /**
     * Writes a sequence of bytes to this channel from a subsequence of the given
     * buffers. This operation, sometimes called a <em>gathering write</em>, is
     * often useful when implementing network protocols that group data into
     * segments consisting of one or more fixed-length headers followed by a
     * variable-length body.
     *
     * The returned result is the number of bytes written.
     *
     * <p>
     * This method writes of up to <i>r</i> bytes to this channel,
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
     * <p>
     * Suppose that a byte sequence of length <i>n</i> is written, where
     * {@code 0}&nbsp;{@code &lt;}&nbsp;<i>n</i>&nbsp;{@code &lt;=}&nbsp;<i>r</i>.
     * Up to the first {@code srcs[offset].remaining()} bytes of this sequence
     * are written from buffer {@code srcs[offset]}, up to the next
     * {@code srcs[offset+1].remaining()} bytes are written from buffer
     * {@code srcs[offset+1]}, and so forth, until the entire byte sequence is
     * written. As many bytes as possible are written from each buffer, hence
     * the final position of each updated buffer, except the last updated
     * buffer, is guaranteed to be equal to that buffer's limit. The underlying
     * operating system may impose a limit on the number of buffers that may be
     * used in an I/O operation. Where the number of buffers (with bytes
     * remaining), exceeds this limit, then the I/O operation is performed with
     * the maximum number of buffers allowed by the operating system.
     *
     * <p>
     * If a timeout is specified and the timeout elapses before the operation
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
    public abstract long write(final ByteBuffer[] srcs, final int offset, final int length, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution;

    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * <p>
     * This writes a
     * sequence of bytes to this channel from the given buffer.
     *
     * The returned result is the number of bytes written.
     *
     * <p>
     * If a timeout is specified and the timeout elapses before the operation
     * completes then the method throws the exception {@link
     * InterruptedByTimeoutException}. Where a timeout occurs, and the
     * implementation cannot guarantee that bytes have not been written, or will
     * not be written to the channel from the given buffer, then further attempts
     * to write to the channel will cause an unspecific runtime exception to be
     * thrown.
     *
     * <p>
     * Otherwise this method works in the same manner as the {@link #write(ByteBuffer)} method.
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
    public abstract int write(final ByteBuffer src, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution;

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public abstract long read(ByteBuffer[] dsts) throws IOException;

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public abstract int read(ByteBuffer dst) throws IOException;

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public abstract long write(ByteBuffer[] srcs, int offset, int length) throws IOException;

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public abstract long write(ByteBuffer[] srcs) throws IOException;

    /**
     * @throws NotYetConnectedException If this channel is not yet connected
     */
    @Override
    @Suspendable
    public abstract int write(final ByteBuffer src) throws IOException;

    @Override
    public abstract boolean isOpen();

    @Override
    public abstract void close() throws IOException;

    /**
     * Shutdown the connection for reading without closing the channel.
     *
     * <p>
     * Once shutdown for reading then further reads on the channel will
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
    public abstract FiberSocketChannel shutdownInput() throws IOException;

    /**
     * Shutdown the connection for writing without closing the channel.
     *
     * <p>
     * Once shutdown for writing then further attempts to write to the
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
    public abstract FiberSocketChannel shutdownOutput() throws IOException;

    /**
     * Returns the remote address to which this channel's socket is connected.
     *
     * <p>
     * Where the channel is bound and connected to an Internet Protocol
     * socket address then the return value from this method is of type {@link
     * java.net.InetSocketAddress}.
     *
     * @return The remote address; {@code null} if the channel's socket is not connected
     *
     * @throws ClosedChannelException If the channel is closed
     * @throws IOException            If an I/O error occurs
     */
    public abstract SocketAddress getRemoteAddress() throws IOException;

    /**
     * @throws ConnectionPendingException
     *                                         If a connection operation is already in progress on this channel
     * @throws AlreadyBoundException           {@inheritDoc}
     * @throws UnsupportedAddressTypeException {@inheritDoc}
     * @throws ClosedChannelException          {@inheritDoc}
     * @throws IOException                     {@inheritDoc}
     */
    @Override
    public abstract FiberSocketChannel bind(SocketAddress local) throws IOException;

    /**
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ClosedChannelException   {@inheritDoc}
     * @throws IOException              {@inheritDoc}
     */
    @Override
    public abstract <T> FiberSocketChannel setOption(SocketOption<T> name, T value) throws IOException;

    @Override
    public abstract SocketAddress getLocalAddress() throws IOException;

    @Override
    public abstract <T> T getOption(SocketOption<T> name) throws IOException;

    @Override
    public abstract Set<SocketOption<?>> supportedOptions();

    /**
     * Returns the IO provider that created this channel.
     * The type of the returned value is implementation dependent, and may be {@code null}.
     */
    public abstract Object provider();
}
