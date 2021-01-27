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
 * A channel's producer-side functional interface.
 *
 * @param Message the type of messages that can be sent to this channel.
 * @author pron
 */
@FunctionalInterface
public interface SendPort<Message> extends Port<Message>, PortAutoCloseable {
    /**
     * Sends a message to the channel, possibly blocking until there's room available in the channel.
     *
     * If the channel is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the channel's {@link Channels.OverflowPolicy OverflowPolicy}, set at construction time.
     *
     * @param message the message
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    default void send(Message message) throws SuspendExecution, InterruptedException {
        send(message, -1, null);
    }

    /**
     * Sends a message to the channel, possibly blocking until there's room available in the channel, but never longer than the
     * specified timeout.
     *
     * If the channel is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the channel's {@link Channels.OverflowPolicy OverflowPolicy}, set at construction time.
     *
     * @param message the message
     * @param timeout the maximum duration this method is allowed to wait.
     * @param unit    the timeout's time unit
     * @return {@code true} if the message has been sent successfully; {@code false} if the timeout has expired.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException;

    /**
     * Sends a message to the channel, possibly blocking until there's room available in the channel, but never longer than the
     * specified timeout.
     *
     * If the channel is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the channel's {@link Channels.OverflowPolicy OverflowPolicy}, set at construction time.
     *
     * @param message the message
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return {@code true} if the message has been sent successfully; {@code false} if the timeout has expired.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    default boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    /**
     * Sends a message to the channel if the channel has room available. This method never blocks.
     *
     * @param message the message
     * @return {@code true} if the message has been sent; {@code false} otherwise.
     */
    default boolean trySend(Message message) {
        try {
            return send(message, 0, TimeUnit.NANOSECONDS);
        } catch (SuspendExecution | InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Closes the channel so that no more messages could be sent to it, and signifies an exception occurred in the producer.
     * The exception will be thrown when the consumer calls {@link ReceivePort}'s {@code receive} or {@code tryReceive},
     * wrapped by a {@link ProducerException}.
     * Messages already sent to the channel prior to calling this method will still be received.
     *
     * @param t the exception causing the close
     */
    default void close(Throwable t) {
    }
}
