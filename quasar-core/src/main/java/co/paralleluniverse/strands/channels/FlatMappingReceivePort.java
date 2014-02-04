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
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import com.google.common.base.Function;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class FlatMappingReceivePort<S, T> implements ReceivePort<T>, DelegatingEquals {
    private final ReceivePort<S> target;
    private final Function<S, ReceivePort<T>> f;
    private ReceivePort<T> port;

    public FlatMappingReceivePort(ReceivePort<S> target, Function<S, ReceivePort<T>> f) {
        if (f == null)
            throw new IllegalArgumentException("f can't be null");
        if (target == null)
            throw new IllegalArgumentException("target can't be null");
        this.target = target;
        this.f = f;
    }

    public FlatMappingReceivePort(ReceivePort<S> target) {
        this(target, null);
    }

    @Override
    @SuppressWarnings("empty-statement")
    public T receive() throws SuspendExecution, InterruptedException {
        for (;;) {
            T m = (port != null ? port.receive() : null);
            if (m != null)
                return m;
            assert port == null || port.isClosed();
            S m0 = target.receive();
            if (m0 == null) // closed
                return null;
            this.port = map(m0);
        }
    }

    @Override
    public T receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        long left = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + left;

        for (;;) {
            T m = (port != null ? port.receive(left, TimeUnit.NANOSECONDS) : null);
            if (m != null)
                return m;

            left = deadline - System.nanoTime();
            if (left <= 0)
                return null;
            assert port == null || port.isClosed();
            
            S m0 = target.receive(left, TimeUnit.NANOSECONDS);
            if (m0 == null)
                return null;
            this.port = map(m0);

            left = deadline - System.nanoTime();
        }
    }

    @Override
    public T tryReceive() {
        for (;;) {
            if (port != null && !port.isClosed())
                return port.tryReceive();
            S m0 = target.tryReceive();
            if (m0 == null)
                return null;
            this.port = map(m0);
        }
    }

    @Override
    public T receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    protected ReceivePort<T> map(S m) {
        return f.apply(m);
    }

    @Override
    public void close() {
        target.close();
    }

    @Override
    public boolean isClosed() {
        return target.isClosed() && (port == null || port.isClosed());
    }

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
