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
import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.StrandFactory;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.SuspendableUtils;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A transforming {@link SendPort} that will apply a reduction function to values.
 *
 * @author circlespainter
 */
class ReducingSendPort<S, T> extends SendPortTransformer<S, T> {
    private static final StrandFactory strandFactoryDefault = DefaultFiberScheduler.getInstance();

    private final Function2<T, S, T> f;
    private final AtomicBoolean closedBeforeFirstSend = new AtomicBoolean(true);
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final StrandFactory strandFactory;
    private T prev;

    public ReducingSendPort(final SendPort<T> target, final Function2<T, S, T> f, final T init, final StrandFactory strandFactory) {
        super(target);
        this.f = f;
        this.prev = init;
        this.strandFactory = strandFactory;
    }

    public ReducingSendPort(final SendPort<T> target, final Function2<T, S, T> f, final T init) {
        this(target, f, init, strandFactoryDefault);
    }

    @Override
    public void close(final Throwable t) {
        if (!closing.getAndSet(true)) {
            if (closedBeforeFirstSend.get()) {
                strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                    @Override
                    public void run() throws SuspendExecution, InterruptedException {
                        target.send(prev);
                        superClose(t);
                    }
                })).start();
            } else {
                superClose(t);
            }
        }
    }

    private void superClose(final Throwable t) {
        if (t != null)
            super.close(t);
        else
            super.close();
    }
    
    @Override
    public void close() {
        close(null);
    }

    @Override
    public boolean trySend(final S message) {
        closedBeforeFirstSend.set(false);
        return super.trySend(message);
    }

    @Override
    public boolean send(final S message, final Timeout timeout) throws SuspendExecution, InterruptedException {
        closedBeforeFirstSend.set(false);
        return super.send(message, timeout);
    }

    @Override
    public boolean send(final S message, final long timeout, final TimeUnit unit) throws SuspendExecution, InterruptedException {
        closedBeforeFirstSend.set(false);
        return super.send(message, timeout, unit);
    }

    @Override
    public void send(final S message) throws SuspendExecution, InterruptedException {
        closedBeforeFirstSend.set(false);
        super.send(message);
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
}