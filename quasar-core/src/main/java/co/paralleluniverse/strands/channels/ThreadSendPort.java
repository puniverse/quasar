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
import java.util.concurrent.TimeUnit;

/**
 * This class is a simple convenience wrapper around {@link SendPort} that can be used by threads (as opposed to fibers). Its methods do not
 * declare they throw {@code SuspendExecution}.
 *
 * @author pron
 */
public class ThreadSendPort<Message> {
    private final SendPort<Message> p;

    /**
     * Creates a new convenience wrapper for using a {@link SendPort} in a thread.
     *
     * @param p the {@link SendPort} to wrap.
     */
    public ThreadSendPort(SendPort<Message> p) {
        this.p = p;
    }

    /**
     * Sends a message to the channel, possibly blocking until there's room available in the channel.
     *
     * If the channel is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the channel's {@link Channels.OverflowPolicy OverflowPolicy}, set at construction time.
     *
     * @param message
     */
    public void send(Message message) throws InterruptedException {
        if (Strand.isCurrentFiber())
            throw new IllegalStateException("This method cannot be called on a fiber");
        try {
            p.send(message);
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

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
     * @return {@code true} if the message has been sent successfully; {@code false} if the timeout has expired.
     */
    public boolean send(Message message, long timeout, TimeUnit unit) throws InterruptedException {
        if (Strand.isCurrentFiber())
            throw new IllegalStateException("This method cannot be called on a fiber");
        try {
            return p.send(message, timeout, unit);
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Sends a message to the channel if the channel has room available. This method never blocks.
     *
     * @param message
     * @return {@code true} if the message has been sent; {@code false} otherwise.
     */
    public boolean trySend(Message message) {
        return p.trySend(message);
    }

    /**
     * Closes the channel so that no more messages could be sent to it.
     */
    public void close() {
        p.close();
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
