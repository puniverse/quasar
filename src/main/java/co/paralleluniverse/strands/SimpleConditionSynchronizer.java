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
package co.paralleluniverse.strands;

import co.paralleluniverse.fibers.SuspendExecution;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jsr166e.ConcurrentHashMapV8;

/**
 *
 * @author pron
 */
public class SimpleConditionSynchronizer {
    private final Collection<Strand> waiters = Collections.newSetFromMap(new ConcurrentHashMapV8<Strand, Boolean>()); // new ConcurrentLinkedQueue<Strand>(); 

    public void await() throws InterruptedException, SuspendExecution {
        waiters.add(Strand.currentStrand());
        Strand.park(this);
        if (Strand.interrupted())
            throw new InterruptedException();
    }

    public long awaitNanos(long timeoutNanos) throws InterruptedException, SuspendExecution {
        waiters.add(Strand.currentStrand());

        final long start = System.nanoTime();
        final long deadline = start + timeoutNanos;

        Strand.parkNanos(this, timeoutNanos);
        if (Strand.interrupted())
            throw new InterruptedException();
        return deadline - System.nanoTime();
    }

    public void await(long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution, TimeoutException {
        waiters.add(Strand.currentStrand());

        final long start = System.nanoTime();
        final long timeoutNanos = unit.toNanos(timeout);
        final long deadline = start + timeoutNanos;

        Strand.parkNanos(this, timeoutNanos);
        if (Strand.interrupted())
            throw new InterruptedException();
        if (System.nanoTime() > deadline)
            throw new TimeoutException();
    }

    public void signalAll() {
        while (!waiters.isEmpty()) {
            for (Iterator<Strand> it = waiters.iterator(); it.hasNext();) {
                final Strand s = it.next();
                Strand.unpark(s);
                it.remove();
            }
        }
    }
}
