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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.ReceivePort.EOFException;
import java.util.concurrent.TimeUnit;

/**
 * This class is a simple convenience wrapper around {@link ReceivePort} that can be used by threads (as opposed to fibers). Its methods do not
 * declare they throw {@code SuspendExecution}.
 *
 * @author pron
 */
public class ThreadReceivePort<Message> {
    private final ReceivePort<Message> p;

    /**
     * Creates a new convenience wrapper for using a {@link ReceivePort} in a thread.
     *
     * @param p the {@link SendPort} to wrap.
     */
    public ThreadReceivePort(ReceivePort<Message> p) {
        this.p = p;
    }

    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available.
     *
     * @return a message, or {@code null} if the channel has been closed and no more messages await (see {@link #isClosed()}).
     * @throws InterruptedException
     */
    public Message receive() throws InterruptedException {
        if (Strand.isCurrentFiber())
            throw new IllegalStateException("This method cannot be called on a fiber");
        try {
            return p.receive();
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available, but no longer than the specified timeout.
     *
     * @param timeout the maximum duration to block waiting for a message.
     * @param unit    the time unit of the timeout.
     * @return a message, or {@code null} if the channel has been closed and no more messages await (see {@link #isClosed()}), or if
     *         the timeout has expired.
     * @throws InterruptedException
     */
    public Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        if (Strand.isCurrentFiber())
            throw new IllegalStateException("This method cannot be called on a fiber");
        try {
            return p.receive(timeout, unit);
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Retrieves a message from the channel if one is available. This method never blocks.
     *
     * @return a message, or {@code null} if one is not immediately available.
     */
    public Message tryReceive() {
        return p.tryReceive();
    }

    /**
     * Closes the channel so that no more messages could be sent to it. Messages already sent to the channel will still be received.
     */
    public void close() {
        p.close();
    }

    /**
     * Tests whether the channel has been closed and no more messages await in the channel. If this method returns {@code true} all
     * future calls to {@link #receive() } are guaranteed to return {@code null}, and calls to {@code receive} on a primitive channel
     * will throw an {@link EOFException EOFException}.
     *
     * @return {@code true} if the channels has been closed and no more messages will be received; {@code false} otherwise.
     */
    public boolean isClosed() {
        return p.isClosed();
    }

    @Override
    public final int hashCode() {
        return p.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return p.equals(obj);
    }

    @Override
    public final String toString() {
        return p.toString();
    }
}
