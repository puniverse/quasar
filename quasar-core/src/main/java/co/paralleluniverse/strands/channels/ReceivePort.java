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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * A channel's consumer-side functional interface.
 *
 * @param Message the type of messages that can be received from this channel.
 * @author pron
 */
@FunctionalInterface
public interface ReceivePort<Message> extends Port<Message>, PortAutoCloseable {
    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available.
     *
     * @return a message, or {@code null} if the channel has been closed and no more messages await (see {@link #isClosed()}).
     * @throws InterruptedException
     * @throws SuspendExecution
     */
    default Message receive() throws SuspendExecution, InterruptedException {
        return receive(-1, null);
    }

    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available, but no longer than the specified timeout.
     *
     * @param timeout the maximum duration to block waiting for a message.
     * @param unit    the time unit of the timeout.
     * @return a message, or {@code null} if the channel has been closed and no more messages await (see {@link #isClosed()}), or if the timeout has expired.
     * @throws InterruptedException
     * @throws SuspendExecution
     */
    Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException;

    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available, but no longer than the specified timeout.
     *
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return a message, or {@code null} if the channel has been closed and no more messages await (see {@link #isClosed()}), or if the timeout has expired.
     * @throws InterruptedException
     * @throws SuspendExecution
     */
    default Message receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    /**
     * Retrieves a message from the channel if one is available. This method never blocks.
     *
     * @return a message, or {@code null} if one is not immediately available.
     */
    default Message tryReceive() {
        try {
            return receive(0, TimeUnit.NANOSECONDS);
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }

    public static class EOFException extends Exception {
        public static EOFException instance = new EOFException();

        private EOFException() {
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }
}
