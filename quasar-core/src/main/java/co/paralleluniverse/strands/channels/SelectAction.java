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

/**
 * A channel operation that is selected by a {@link Selector}.
 *
 * @author pron
 */
public abstract class SelectAction<Message> {
    final Selectable<Message> port;
    int index;
    Message item;
    volatile boolean done;
    
    SelectAction(Selectable<Message> port) {
        this.port = port;
    }

    /**
     * Returns the message to send if this is a send operation, or the message that has been received if this is a receive operation and has been
     * successfully completed by the selector.
     *
     * @return the message to send if this is a send operation, or the message that has been received if this is a receive operation and has been
     *         successfully completed by the selector.
     */
    public Message message() {
        return item;
    }

    public int index() {
        return index;
    }

    /**
     * Returns the channel for this operation.
     *
     * @return the channel for this operation.
     */
    public Port<Message> port() {
        return (Port<Message>) port;
    }

    /**
     * Tests whether this operation is the one operation that has been selected and completed by the selector.
     *
     * @return {@code true} if this operation is the one operation that has been selected and completed by the selector; {@code false} otherwise.
     */
    public boolean isDone() {
        return done;
    }
}
