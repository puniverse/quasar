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
import java.util.concurrent.TimeoutException;

/**
 * A primitive {@code float} channel's consumer-side interface.
 *
 * <b>All methods of this interface must only be called by the channel's owner.</b>
 *
 * @author pron
 */
public interface FloatReceivePort extends ReceivePort<Float> {
    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available.
     * If the channel has been closed and no more messages await, this method throws an {@link EOFException}.
     *
     * @return a message.
     * @throws EOFException if the channel has been closed and no more messages await
     * @throws InterruptedException
     */
    float receiveFloat() throws SuspendExecution, InterruptedException, EOFException;

    /**
     * Retrieves a message from the channels, possibly blocking until one becomes available, but no longer than the specified timeout.
     * If the channel has been closed and no more messages await, this method throws an {@link EOFException}.
     *
     * @param timeout the maximum duration to block waiting for a message.
     * @param unit the time unit of the timeout.
     * @return a message. (see {@link #isClosed()}), or if the timeout has expired.
     * @throws TimeoutException if the timeout has expired
     * @throws EOFException if the channel has been closed and no more messages await
     * @throws InterruptedException
     */
    float receiveFloat(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException, EOFException;
}
