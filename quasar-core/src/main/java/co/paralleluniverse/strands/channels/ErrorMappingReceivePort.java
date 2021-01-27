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

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import com.google.common.base.Function;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
class ErrorMappingReceivePort<T> extends DelegatingReceivePort1<T, T> implements ReceivePort<T> {
    private final Function<Exception, T> f;
    private boolean done;

    public ErrorMappingReceivePort(ReceivePort<T> target, Function<Exception, T> f) {
        super(target);
        this.f = f;
    }

    public ErrorMappingReceivePort(ReceivePort<T> target) {
        this(target, null);
    }

    protected T map(Exception e) {
        if (f != null) {
            T res = f.apply(e);
            this.done = isClosed();
            return res;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("empty-statement")
    public T receive() throws SuspendExecution, InterruptedException {
        try {
            return target.receive();
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            return done ? null : map(e);
        }
    }

    @Override
    public T tryReceive() {
        try {
            return target.tryReceive();
        } catch (Exception e) {
            return done ? null : map(e);
        }
    }

    @Override
    public T receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        try {
            return target.receive(timeout, unit);
        } catch (Exception e) {
            return done ? null : map(e);
        }
    }

    @Override
    public T receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        try {
            return target.receive(timeout);
        } catch (Exception e) {
            return done ? null : map(e);
        }
    }
}
