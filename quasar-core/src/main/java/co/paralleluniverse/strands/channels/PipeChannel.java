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
 * Sends all messages to the given pipe channel, but equals target
 */
class PipeChannel<T> implements SendPort<T>, DelegatingEquals {
    private final SendPort<T> pipe;
    private final SendPort<?> target;

    public PipeChannel(SendPort<T> pipe, SendPort<?> target) {
        this.pipe = pipe;
        this.target = target;
    }
    
    @Override
    public void close() {
        pipe.close();
    }

    @Override
    public void close(Throwable t) {
        pipe.close(t);
    }

    @Override
    public void send(T message) throws SuspendExecution, InterruptedException {
        pipe.send(message);
    }

    @Override
    public boolean send(T message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return pipe.send(message, timeout, unit);
    }

    @Override
    public boolean send(T message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return pipe.send(message, timeout);
    }

    @Override
    public boolean trySend(T message) {
        return pipe.trySend(message);
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
