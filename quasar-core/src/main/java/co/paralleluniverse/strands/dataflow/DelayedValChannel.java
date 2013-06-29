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
package co.paralleluniverse.strands.dataflow;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Condition;
import co.paralleluniverse.strands.DoneSynchronizer;
import co.paralleluniverse.strands.channels.ReceivePort;
import co.paralleluniverse.strands.channels.SelectableReceive;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An adapter that turns a DelayedVal into a ReceivePort that receives the DelayedVal's value and then closes.
 * @author pron
 */
public class DelayedValChannel<V> implements ReceivePort<V>, SelectableReceive {
    private final DelayedVal<V> dv;
    private final AtomicBoolean closed = new AtomicBoolean();

    public DelayedValChannel(DelayedVal<V> val) {
        this.dv = val;
    }

    @Override
    public Condition receiveSelector() {
        Condition sync = dv.getSync();
        return sync != null ? sync : DoneSynchronizer.instance;
    }

    @Override
    public V receive() throws SuspendExecution, InterruptedException {
        if (closed.get())
            return null;
        final V v = dv.get();
        close();
        return v;
    }

    @Override
    public V tryReceive() {
        if (closed.get())
            return null;
        if (!dv.isDone())
            return null;
        final V v = dv.getValue();
        close();
        return v;
    }

    @Override
    public V receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (closed.get())
            return null;
        final V v;
        try {
            v = dv.get(timeout, unit);
            close();
            return v;
        } catch (TimeoutException ex) {
            return null;
        }
    }

    @Override
    public void close() {
        closed.set(true);
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }
}
