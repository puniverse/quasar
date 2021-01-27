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

import co.paralleluniverse.common.util.Function2;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A transforming {@link ReceivePort} that will apply a reduction function to values.
 *
 * @author circlespainter
 */
class ReducingReceivePort<S, T> extends ReceivePortTransformer<S, T> implements ReceivePort<T> {
    private final Function2<T, S, T> f;
    private final AtomicBoolean receivedAtLeastOnce = new AtomicBoolean(false);
    private T prev;

    public ReducingReceivePort(final ReceivePort<S> target, final Function2<T, S, T> f, T init) {
        super(target);
        this.f = f;
        this.prev = init;
    }

    @Override
    public T receive(final Timeout timeout) throws SuspendExecution, InterruptedException {
        return checkReceivedAtLeastOnce(super.receive(timeout));
    }

    @Override
    public T tryReceive() {
        return checkReceivedAtLeastOnce(super.tryReceive());
    }

    @Override
    public T receive() throws SuspendExecution, InterruptedException {
        return checkReceivedAtLeastOnce(super.receive());
    }

    @Override
    public boolean isClosed() {
        return super.isClosed() && receivedAtLeastOnce.get();
    }

    @Override
    public T receive(final long timeout, final TimeUnit unit) throws SuspendExecution, InterruptedException {
        return checkReceivedAtLeastOnce(super.receive(timeout, unit));
    }

    @Override
    protected T transform(final S m) {
        return (this.prev = reduce(prev, m));
    }

    private T reduce(final T prev, final S m) {
        if (f != null && prev != null)
            return f.apply(prev, m);
        throw new UnsupportedOperationException();
    }

    private T checkReceivedAtLeastOnce(final T r) {
        T ret = r;
        if (target.isClosed() && !receivedAtLeastOnce.get())
            ret = prev;
        receivedAtLeastOnce.set(true);
        return ret;
    }
}
