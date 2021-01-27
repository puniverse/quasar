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
import java.nio.channels.*;
import java.util.Set;

/**
 * A fiber-blocking version of {@link ServerSocketChannel}.
 *
 * @author pron
 */
public abstract class FiberServerSocketChannel implements NetworkChannel {
    /**
     * Opens an server-socket channel.
     *
     * <p>
     * If the group parameter is {@code null} then the
     * resulting channel is created by the system-wide default provider, and
     * bound to the <em>default group</em>.
     *
     * @param group The group to which the newly constructed channel should be bound, or {@code null} for the default group
     *
     * @return A new server socket channel
     *
     * @throws ShutdownChannelGroupException If the channel group is shutdown
     * @throws IOException                   If an I/O error occurs
     */
    public static FiberServerSocketChannel open(ChannelGroup group) throws IOException, SuspendExecution {
        if (group == null)
            group = ChannelGroup.defaultGroup();
        return group.newFiberServerSocketChannel();
    }

    /**
     * Opens a server-socket channel.
     * Same as {@link #open(java.nio.channels.ChannelGroup) open((AsynchronousChannelGroup) null)}.
     *
     * @return A new server socket channel
     * @throws IOException If an I/O error occurs
     */
    public static FiberServerSocketChannel open() throws IOException, SuspendExecution {
        return open(null);
    }

    /**
     * Accepts a connection.
     *
     * <p>
     * This method accepts a
     * connection made to this channel's socket. The returned result is
     * the {@link FiberSocketChannel} to the new connection.
     *
     * <p>
     * When a new connection is accepted then the resulting {@code
     * FiberSocketChannel} will be bound to the same {@link
     * ChannelGroup} as this channel. If the group is {@link
     * AsynchronousChannelGroup#isShutdown shutdown} and a connection is accepted,
     * then the connection is closed, and the method throws an {@code
     * IOException} with cause {@link ShutdownChannelGroupException}.
     *
     * <p>
     * If a security manager has been installed then it verifies that the
     * address and port number of the connection's remote endpoint are permitted
     * by the security manager's {@link SecurityManager#checkAccept checkAccept}
     * method. The permission check is performed with privileges that are restricted
     * by the calling context of this method. If the permission check fails then
     * the connection is closed and the operation completes with a {@link
     * SecurityException}.
     *
     * @return the {@link FiberSocketChannel} to the new connection.
     *
     * @throws AcceptPendingException        If an accept operation is already in progress on this channel
     * @throws NotYetBoundException          If this channel's socket has not yet been bound
     * @throws ShutdownChannelGroupException If the channel group has terminated
     */
    public abstract FiberSocketChannel accept() throws IOException, SuspendExecution;

    @Override
    public abstract boolean isOpen();

    @Override
    public abstract void close() throws IOException;

    /**
     * Binds the channel's socket to a local address and configures the socket to
     * listen for connections.
     *
     * <p>
     * An invocation of this method is equivalent to the following:
     * <blockquote><pre>
     * bind(local, 0);
     * </pre></blockquote>
     *
     * @param local The local address to bind the socket, or {@code null} to bind
     *              to an automatically assigned socket address
     *
     * @return This channel
     *
     * @throws AlreadyBoundException           {@inheritDoc}
     * @throws UnsupportedAddressTypeException {@inheritDoc}
     * @throws SecurityException               {@inheritDoc}
     * @throws ClosedChannelException          {@inheritDoc}
     * @throws IOException                     {@inheritDoc}
     */
    @Override
    public abstract FiberServerSocketChannel bind(SocketAddress local) throws IOException;

    /**
     * Binds the channel's socket to a local address and configures the socket to
     * listen for connections.
     *
     * <p>
     * This method is used to establish an association between the socket and
     * a local address. Once an association is established then the socket remains
     * bound until the associated channel is closed.
     *
     * <p>
     * The {@code backlog} parameter is the maximum number of pending
     * connections on the socket. Its exact semantics are implementation specific.
     * In particular, an implementation may impose a maximum length or may choose
     * to ignore the parameter altogther. If the {@code backlog} parameter has
     * the value {@code 0}, or a negative value, then an implementation specific
     * default is used.
     *
     * @param local   The local address to bind the socket, or {@code null} to bind
     *                to an automatically assigned socket address
     * @param backlog The maximum number of pending connections
     *
     * @return This channel
     *
     * @throws AlreadyBoundException           If the socket is already bound
     * @throws UnsupportedAddressTypeException If the type of the given address is not supported
     * @throws SecurityException               If a security manager has been installed and its {@link
     *          SecurityManager#checkListen checkListen} method denies the operation
     * @throws ClosedChannelException          If the channel is closed
     * @throws IOException                     If some other I/O error occurs
     */
    public abstract FiberServerSocketChannel bind(SocketAddress local, int backlog) throws IOException;

    /**
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ClosedChannelException   {@inheritDoc}
     * @throws IOException              {@inheritDoc}
     */
    @Override
    public abstract <T> FiberServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException;

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
