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
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A primitive {@code long} channel's producer-side interface.
 *
 * @author pron
 */
public interface LongSendPort extends SendPort<Long> {
    /**
     * Sends a message to the channel, possibly blocking until there's room available in the channel.
     *
     * If the channel is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the channel's {@link Channels.OverflowPolicy OverflowPolicy}, set at construction time.
     *
     * @param message
     * @throws SuspendExecution
     */
    void send(long message) throws SuspendExecution, InterruptedException;

    /**
     * Sends a message to the channel, possibly blocking until there's room available in the channel, but never longer than the
     * specified timeout.
     *
     * If the channel is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the channel's {@link Channels.OverflowPolicy OverflowPolicy}, set at construction time.
     *
     * @param message
     * @param timeout the maximum duration this method is allowed to wait.
     * @param unit    the timeout's time unit
     * @return {@code true} if the message has been sent successfully; {@code false} if the timeout has expired.
     * @throws SuspendExecution
     */
    boolean send(long message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException;

    /**
     * Sends a message to the channel, possibly blocking until there's room available in the channel, but never longer than the
     * specified timeout.
     *
     * If the channel is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the channel's {@link Channels.OverflowPolicy OverflowPolicy}, set at construction time.
     *
     * @param message
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return {@code true} if the message has been sent successfully; {@code false} if the timeout has expired.
     * @throws SuspendExecution
     */
    boolean send(long message, Timeout timeout) throws SuspendExecution, InterruptedException, TimeoutException;

    /**
     * Sends a message to the channel if the channel has room available. This method never blocks.
     *
     * @param message
     * @return {@code true} if the message has been sent; {@code false} otherwise.
     */
    boolean trySend(long message);
}
