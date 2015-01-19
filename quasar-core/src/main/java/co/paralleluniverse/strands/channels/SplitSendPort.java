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
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * {@link SendPort} that will send messages it receives to a target {@link SendPort}. Concrete subclasses will need to implement {@code select} yielding
 * {@link SendPort} to send the message to.
 * <p/>
 * @author circlespainter
 */
abstract class SplitSendPort<Message> implements SendPort<Message> {
    private boolean closed = false;
    
    /**
     * Subclasses will implement this method to select the target {@link SendPort}.
     */
    protected abstract SendPort<? super Message> select(Message message);

    @Override
    public void close(Throwable t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean trySend(Message message) {
        if (!closed)
            return select(message).trySend(message);

        return true;
    }

    @Override
    public boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        if (!closed)
            return select(message).send(message, timeout);

        return true;
    }

    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (!closed)
            return select(message).send(message, timeout, unit);

        return true;
    }

    @Override
    public void send(Message message) throws SuspendExecution, InterruptedException {
        if (!closed)
            select(message).send(message);
    }
}
