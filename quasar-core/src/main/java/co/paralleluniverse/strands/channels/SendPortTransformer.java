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

import co.paralleluniverse.common.util.DelegatingEquals;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
abstract class SendPortTransformer<S, T> implements SendPort<S>, DelegatingEquals {
    protected final SendPort<T> target;

    public SendPortTransformer(SendPort<T> target) {
        if (target == null)
            throw new IllegalArgumentException("Target port may not be null");
        this.target = target;
    }

    @Override
    public void send(S message) throws SuspendExecution, InterruptedException {
        final T m = transform(message);
        if (m != null)
            target.send(m);
    }

    @Override
    public boolean send(S message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        final T m = transform(message);
        if (m != null)
            return target.send(m, timeout, unit);
        return true;
    }

    @Override
    public boolean send(S message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean trySend(S message) {
        final T m = transform(message);
        if (m != null)
            return target.trySend(m);
        return true;
    }

    @Override
    public void close() {
        target.close();
    }

    @Override
    public void close(Throwable t) {
        target.close(t);
    }

    protected abstract T transform(S m);

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DelegatingEquals)
            return obj.equals(target);
        else
            return target.equals(obj);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) + "{" + target + "}";
    }
}
