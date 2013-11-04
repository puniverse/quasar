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
import java.util.concurrent.TimeUnit;

/**
 * A channel's producer-side interface.
 * 
 * @author pron
 */
public interface SendPort<Message> extends Port<Message>, AutoCloseable {
    /**
     * Sends a message to the channel, possibly blocking until there's room available in the channel.
     *
     * If the channel is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the channel's {@link Channels.OverflowPolicy OverflowPolicy}, set at construction time.
     *
     * @param message
     * @throws SuspendExecution
     */
    void send(Message message) throws SuspendExecution, InterruptedException;

    /**
     * Sends a message to the channel, possibly blocking until there's room available in the channel, but never longer than the 
     * specified timeout.
     *
     * If the channel is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the channel's {@link Channels.OverflowPolicy OverflowPolicy}, set at construction time.
     *
     * @param message
     * @param timeout the maximum duration this method is allowed to wait.
     * @param unit the timeout's time unit
     * @return {@code true} if the message has been sent successfully; {@code false} if the timeout has elapsed.
     * @throws SuspendExecution
     */
    boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException;

    /**
     * Sends a message to the channel if the channel has room available. This method never blocks.
     * @param message
     * @return {@code true} if the message has been sent; {@code false} otherwise.
     * @return 
     */
    boolean trySend(Message message);

    /**
     * Closes the channel so that no more messages could be sent to it.
     */
    @Override
    void close();
}
