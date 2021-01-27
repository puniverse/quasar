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

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * {@link SendPort} that will send messages it receives to a target {@link SendPort}. Concrete subclasses will need to implement {@code select} yielding
 * {@link SendPort} to send the message to.
 *
 * @author circlespainter
 */
public abstract class SplitSendPort<Message> implements SendPort<Message> {
    private volatile boolean closed = false;
    
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
        try {
            return send(message, -1, TimeUnit.NANOSECONDS);
        } catch (Throwable t) {
            // This should never happen
            throw new AssertionError(t);
        }
    }

    @Override
    public void send(Message message) throws SuspendExecution, InterruptedException {
        send(message, -1, null);
    }

    @Override
    public boolean send(final Message message, final Timeout timeout) throws SuspendExecution, InterruptedException {
        return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean send(final Message message, final long timeout, final TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (!closed) {
            final SendPort<? super Message> target = select(message);
            if (unit == null)
                // Untimed send
                target.send(message);
            else if (timeout > 0)
                // Timed send
                return target.send(message, timeout, unit);
            else
                // trySend
                return target.trySend(message);
        }

        return false;
    }
}
