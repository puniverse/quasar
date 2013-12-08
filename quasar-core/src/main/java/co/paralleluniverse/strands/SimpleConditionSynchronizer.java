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

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author pron
 */
public class SimpleConditionSynchronizer extends ConditionSynchronizer implements Condition {
    private final Queue<Strand> waiters = new ConcurrentLinkedQueue<Strand>();

    public SimpleConditionSynchronizer(Object owner) {
        super(owner);
    }

    @Override
    public Object register() {
        final Strand currentStrand = Strand.currentStrand();
        record("register", "%s register %s", this, currentStrand);
        waiters.add(currentStrand);
        return null;
    }

    @Override
    public void unregister(Object registrationToken) {
        final Strand currentStrand = Strand.currentStrand();
        record("unregister", "%s unregister %s", this, currentStrand);
        if (!waiters.remove(currentStrand))
            throw new IllegalMonitorStateException();
    }

    @Override
    public void signalAll() {
        for (Iterator<Strand> it = waiters.iterator(); it.hasNext();) {
            final Strand s = it.next();
            record("signalAll", "%s signalling %s", this, s);

            Strand.unpark(s, owner);
        }
    }

    @Override
    public void signal() {
        final Strand s = waiters.peek();
        if (s != null) {
            record("signal", "%s signalling %s", this, s);
            Strand.unpark(s, owner);
        }
    }
}
