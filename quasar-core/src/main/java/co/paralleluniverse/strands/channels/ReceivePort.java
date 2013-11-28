/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * A channel's consumer-side interface.
 *
 * @param Message the type of messages that can be received from this channel.
 * @author pron
 */
public interface ReceivePort<Message> extends Port<Message> {
    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available.
     *
     * @return a message, or {@code null} if the channel has been closed and no more messages await (see {@link #isClosed()}).
     * @throws InterruptedException
     */
    Message receive() throws SuspendExecution, InterruptedException;

    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available, but no longer than the specified timeout.
     *
     * @param timeout the maximum duration to block waiting for a message.
     * @param unit the time unit of the timeout.
     * @return a message, or {@code null} if the channel has been closed and no more messages await (see {@link #isClosed()}), or if the timeout has expired.
     * @throws InterruptedException
     */
    Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException;

    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available, but no longer than the specified timeout.
     *
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return a message, or {@code null} if the channel has been closed and no more messages await (see {@link #isClosed()}), or if the timeout has expired.
     * @throws InterruptedException
     */
    Message receive(Timeout timeout) throws SuspendExecution, InterruptedException;

    /**
     * Retrieves a message from the channel if one is available. This method never blocks.
     *
     * @return a message, or {@code null} if one is not immediately available.
     */
    Message tryReceive();

    /**
     * Closes the channel so that no more messages could be sent to it. Messages already sent to the channel will still be received.
     */
    void close();

    /**
     * Tests whether the channel has been closed and no more messages await in the channel. If this method returns {@code true} all
     * future calls to {@link #receive() } are guaranteed to return {@code null}, and calls to {@code receive} on a primitive channel
     * will throw a {@link EOFException}.
     *
     * @return {@code true} if the channels has been closed and no more messages will be received; {@code false} otherwise.
     */
    boolean isClosed();

    public static class EOFException extends Exception {
        public EOFException() {
        }
    }
}
