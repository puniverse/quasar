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

import co.paralleluniverse.fibers.SuspendExecution;

/**
 * Various useful operations between various {@link Port} instances.
 * <p/>
 * @author circlespainter
 */
public class ChannelOps {
    /**
     * Receives one message from a {@link ReceivePort} and sends it to a {@link SendPort}.
     * <p/>
     * @return {@code true} If the send has succeeded, {@code false} if the {@link ReceivePort} was closed.
     */
    static <Message> boolean transfer(final ReceivePort<Message> from, final SendPort<Message> to) throws SuspendExecution, InterruptedException {
        if (!from.isClosed()) {
            to.send(from.receive());
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Trasfers all messages from a {@link ReceivePort} to a {@link SendPort} until the {@link ReceivePort} is closed.
     */
    static <Message> void pipe(final ReceivePort<Message> from, final SendPort<Message> to) throws SuspendExecution, InterruptedException {
        while(!from.isClosed())
            transfer(from, to);
    }
}
