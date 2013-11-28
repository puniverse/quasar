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
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public abstract class TransformingReceivePort<S, T> implements ReceivePort<T> {
    private final ReceivePort<S> target;

    public TransformingReceivePort(ReceivePort<S> target) {
        this.target = target;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public T receive() throws SuspendExecution, InterruptedException {
        for (;;) {
            S m0 = target.receive();
            if (m0 == null) // closed
                return null;
            T m = transform(m0);
            if (m != null)
                return m;
        }
    }

    @Override
    public T tryReceive() {
        for (;;) {
            S m0 = target.tryReceive();
            if (m0 == null)
                return null;
            T m = transform(m0);
            if (m != null)
                return m;
        }
    }

    @Override
    public T receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        long left = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + left;

        for (;;) {
            S m0 = target.receive(left, TimeUnit.NANOSECONDS);
            if (m0 == null)
                return null;
            T m = transform(m0);
            if (m != null)
                return m;
            left = deadline - System.nanoTime();
        }
    }

    @Override
    public T receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }
    
    @Override
    public void close() {
        target.close();
    }

    @Override
    public boolean isClosed() {
        return target.isClosed();
    }

    protected abstract T transform(S m);
}
