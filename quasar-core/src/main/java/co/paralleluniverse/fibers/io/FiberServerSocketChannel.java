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
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.Set;

/**
 * Uses an {@link AsynchronousServerSocketChannel} to implement a fiber-blocking version of {@link ServerSocketChannel}.
 *
 * @author pron
 */
public class FiberServerSocketChannel implements NetworkChannel {
    private final AsynchronousServerSocketChannel ac;

    public FiberServerSocketChannel(AsynchronousServerSocketChannel assc) {
        this.ac = assc;
    }

    /**
     * Opens a server-socket channel.
     * Same as {@link #open(java.nio.channels.AsynchronousChannelGroup) open((AsynchronousChannelGroup) null)}.
     *
     * @return A new server socket channel
     * @throws IOException If an I/O error occurs
     */
    public static FiberServerSocketChannel open() throws IOException {
        return new FiberServerSocketChannel(AsynchronousServerSocketChannel.open(FiberAsyncIO.defaultGroup()));
    }

    /**
     * Opens an server-socket channel.
     *
     * <p> The new channel is created by invoking the {@link
     * java.nio.channels.spi.AsynchronousChannelProvider#openAsynchronousServerSocketChannel
     * openAsynchronousServerSocketChannel} method on the {@link
     * java.nio.channels.spi.AsynchronousChannelProvider} object that created
     * the given group. If the group parameter is <tt>null</tt> then the
     * resulting channel is created by the system-wide default provider, and
     * bound to the <em>default group</em>.
     *
     * @param group The group to which the newly constructed channel should be bound, or <tt>null</tt> for the default group
     *
     * @return A new asynchronous server socket channel
     *
     * @throws ShutdownChannelGroupException If the channel group is shutdown
     * @throws IOException                   If an I/O error occurs
     */
    public static FiberServerSocketChannel open(AsynchronousChannelGroup group) throws IOException {
        return new FiberServerSocketChannel(AsynchronousServerSocketChannel.open(group != null ? group : FiberAsyncIO.defaultGroup()));
    }

    /**
     * Accepts a connection.
     *
     * <p> This method accepts a
     * connection made to this channel's socket. The returned result is
     * the {@link FiberSocketChannel} to the new connection.
     *
     * <p> When a new connection is accepted then the resulting {@code
     * FiberSocketChannel} will be bound to the same {@link
     * AsynchronousChannelGroup} as this channel. If the group is {@link
     * AsynchronousChannelGroup#isShutdown shutdown} and a connection is accepted,
     * then the connection is closed, and the method throws an {@code
     * IOException} with cause {@link ShutdownChannelGroupException}.
     *
     * <p> If a security manager has been installed then it verifies that the
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
    public FiberSocketChannel accept() throws IOException, SuspendExecution {
        return new FiberSocketChannel(new FiberAsyncIO<AsynchronousSocketChannel>() {
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

    /**
     * Binds the channel's socket to a local address and configures the socket to
     * listen for connections.
     *
     * <p> An invocation of this method is equivalent to the following:
     * <blockquote><pre>
     * bind(local, 0);
     * </pre></blockquote>
     *
     * @param local The local address to bind the socket, or <tt>null</tt> to bind
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
    public FiberServerSocketChannel bind(SocketAddress local) throws IOException {
        ac.bind(local);
        return this;
    }

    /**
     * Binds the channel's socket to a local address and configures the socket to
     * listen for connections.
     *
     * <p> This method is used to establish an association between the socket and
     * a local address. Once an association is established then the socket remains
     * bound until the associated channel is closed.
     *
     * <p> The {@code backlog} parameter is the maximum number of pending
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
    public FiberServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        ac.bind(local, backlog);
        return this;
    }

    /**
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws ClosedChannelException   {@inheritDoc}
     * @throws IOException              {@inheritDoc}
     */
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
