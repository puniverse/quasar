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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A primitive {@code long} channel's consumer-side interface.
 *
 * <b>All methods of this interface must only be called by the channel's owner.</b>
 *
 * @author pron
 */
public interface LongReceivePort extends ReceivePort<Long> {
    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available.
     * If the channel has been closed and no more messages await, this method throws an {@link EOFException}.
     *
     * @return a message.
     * @throws ReceivePort.EOFException if the channel has been closed and no more messages await
     * @throws InterruptedException
     */
    long receiveLong() throws SuspendExecution, InterruptedException, EOFException;

    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available, but no longer than the specified timeout.
     * If the channel has been closed and no more messages await, this method throws an {@link EOFException}.
     *
     * @param timeout the maximum duration to block waiting for a message.
     * @param unit    the time unit of the timeout.
     * @return a message. (see {@link #isClosed()}), or if the timeout has expired.
     * @throws TimeoutException         if the timeout has expired
     * @throws ReceivePort.EOFException if the channel has been closed and no more messages await
     * @throws InterruptedException
     */
    long receiveLong(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException, EOFException;

    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available, but no longer than the specified timeout.
     * If the channel has been closed and no more messages await, this method throws an {@link EOFException}.
     *
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return a message. (see {@link #isClosed()}), or if the timeout has expired.
     * @throws TimeoutException         if the timeout has expired
     * @throws ReceivePort.EOFException if the channel has been closed and no more messages await
     * @throws InterruptedException
     */
    long receiveLong(Timeout timeout) throws SuspendExecution, InterruptedException, TimeoutException, EOFException;

    /**
     * Tests whether a value is pending in the channel. If it is, the next call to {@code receiveDouble} is guaranteed not to block.
     * 
     * @return {@code true} if a value is waiting in the channel; {@code false} otherwise.
     */
    boolean hasMessage();
}
