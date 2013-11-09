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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Sincle consumer
 *
 * @author pron
 */
public abstract class ZippingReceivePort<Message> implements ReceivePort<Message> {
    private final ReceivePort<?>[] targets;
    private final Object[] ms;

    public ZippingReceivePort(ReceivePort<?>[] targets) {
        this.targets = Arrays.copyOf(targets, targets.length);
        this.ms = new Object[targets.length];
    }

    @Override
    @SuppressWarnings("empty-statement")
    public Message receive() throws SuspendExecution, InterruptedException {
        for (int i = 0; i < targets.length; i++) {
            if (ms[i] == null) {
                Object m = targets[i].receive();
                if (m == null) // closed
                    return null;
                ms[i] = m;
            }
        }
        return transform(ms);
    }

    @Override
    public Message tryReceive() {
        for (int i = 0; i < targets.length; i++) {
            if (ms[i] == null) {
                Object m = targets[i].tryReceive();
                if (m == null)
                    return null;
                ms[i] = m;
            }
        }
        return transform(ms);
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        long left = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + left;

        for (int i = 0; i < targets.length; i++) {
            if (ms[i] == null) {
                Object m = targets[i].receive(left, TimeUnit.NANOSECONDS);
                if (m == null)
                    return null;
                ms[i] = m;
                left = deadline - System.nanoTime();
            }
        }
        return transform(ms);
    }

    @Override
    public void close() {
        for (ReceivePort<?> c : targets)
            c.close();
    }

    @Override
    public boolean isClosed() {
        for (ReceivePort<?> c : targets) {
            if (c.isClosed())
                return true;
        }
        return false;
    }

    protected abstract Message transform(Object[] ms);
}