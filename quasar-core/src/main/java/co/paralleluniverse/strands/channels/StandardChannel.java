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

/**
 * A common interface for standard channel implementations
 * @author pron
 */
public interface StandardChannel<Message> extends Channel<Message> {
    /**
     * The channel's internal buffer capacity.
     * @return the channel's internal buffer capacity, {@code -1} for an unbounded buffer, and {@code 0} for a transfer channel.
     */
    int capacity();

    /**
     * Whether or not the channel supports a single producer only.
     * @return {@code true} if the channel supports no more than one producer; {@code false} otherwise.
     */
    boolean isSingleProducer();

    /**
     * Whether or not the channel supports a single consumer only.
     * @return {@code true} if the channel supports no more than one consumer; {@code false} otherwise.
     */
    boolean isSingleConsumer();
}
