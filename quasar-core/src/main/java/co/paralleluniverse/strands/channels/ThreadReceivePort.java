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
import co.paralleluniverse.strands.Strand;
import java.util.concurrent.TimeUnit;

/**
 * This class is a simple convenience wrapper around {@link ReceivePort} that can be used by threads (as opposed to fibers). Its methods do not
 * declare they throw {@code SuspendExecution}.
 *
 * @author pron
 */
public class ThreadReceivePort<Message> {
    private final ReceivePort<Message> p;

    public ThreadReceivePort(ReceivePort<Message> p) {
        this.p = p;
    }

    public Message receive() throws InterruptedException {
        if (Strand.isCurrentFiber())
            throw new IllegalStateException("This method cannot be called on a fiber");
        try {
            return p.receive();
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    public Message tryReceive() {
        return p.tryReceive();
    }

    public Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        if (Strand.isCurrentFiber())
            throw new IllegalStateException("This method cannot be called on a fiber");
        try {
            return p.receive(timeout, unit);
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    public boolean isClosed() {
        return p.isClosed();
    }

    public void close() {
        p.close();
    }

    @Override
    public int hashCode() {
        return p.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return p.equals(obj);
    }

    @Override
    public String toString() {
        return p.toString();
    }
}
