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

public interface PortAutoCloseable extends AutoCloseable {
    /**
     * Closes the channel so that no more messages could be sent to it. Messages already sent to the channel will still be received.
     */
    @Override
    default void close() {
    }

    /**
     * Tests whether the channel has been closed and no more messages await in the channel.
     *
     * If this method returns {@code true} all
     * future calls to {@link #receive() receive} are guaranteed to return {@code null}, and calls to {@code receive} on a primitive channel
     * will throw a {@link EOFException}.
     *
     * @return {@code true} if the channels has been closed and no more messages will be received; {@code false} otherwise.
     */
    default boolean isClosed() {
        return false;
    }
}
