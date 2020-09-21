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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import com.google.common.base.Function;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Single consumer
 *
 * @author pron
 */
public class ZippingReceivePort<Message> implements ReceivePort<Message> {
    private final Function<Object[], Message> f;
    private final ReceivePort<?>[] targets;
    private Object[] ms;

    public ZippingReceivePort(Function<Object[], Message> f, ReceivePort<?>... targets) {
        this(f, Arrays.asList(targets));
    }

    public ZippingReceivePort(Function<Object[], Message> f, List<? extends ReceivePort<?>> targets) {
        this.f = f;
        this.targets = targets.toArray(new ReceivePort[targets.size()]);
        this.ms = new Object[targets.size()];
    }

    public ZippingReceivePort(ReceivePort<?>... targets) {
        this(null, targets);
    }

    public ZippingReceivePort(List<? extends ReceivePort<?>> targets) {
        this(null, targets);
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
        return transformAndReset();
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
        return transformAndReset();
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
        return transformAndReset();
    }

    @Override
    public Message receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    private Message transformAndReset() {
        final Object[] ms1 = copy(ms);
        Arrays.fill(ms, null);
        return transform(ms1);
    }

    private static Object[] copy(Object[] array) {
        Object[] array2 = new Object[array.length];
        System.arraycopy(array, 0, array2, 0, array.length);
        return array2;
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

    protected Message transform(Object[] ms) {
        if (f != null)
            return f.apply(ms);
        throw new UnsupportedOperationException();
    }
}