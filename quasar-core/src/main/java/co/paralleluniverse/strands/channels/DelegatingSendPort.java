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

import co.paralleluniverse.common.util.DelegatingEquals;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;

public class DelegatingSendPort<T> implements SendPort<T>, DelegatingEquals {
    protected final SendPort<T> target;

    public DelegatingSendPort(SendPort<T> target) {
        if (target == null)
            throw new IllegalArgumentException("target can't be null");
        this.target = target;
    }

    @Override
    public void send(T message) throws SuspendExecution, InterruptedException {
        target.send(message);
    }

    @Override
    public boolean send(T message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return target.send(message, timeout, unit);
    }

    @Override
    public boolean send(T message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return target.send(message, timeout);
    }

    @Override
    public boolean trySend(T message) {
        return target.trySend(message);
    }

    @Override
    public void close(Throwable t) {
        target.close(t);
    }

    @Override
    public void close() {
        target.close();
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return Channels.delegatingEquals(target, obj);
    }

    @Override
    public String toString() {
        return Channels.delegatingToString(this, target);
    }
}
