/*
 * Copyright (c) 2011-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.common.monitoring;

//import java.util.concurrent.atomic.AtomicLong;
import jersey.repackaged.jsr166e.LongAdder;

/**
 *
 * @author pron
 */
public class Counter {
    // private final AtomicLong al = new AtomicLong();
    private final LongAdder la = new LongAdder();

    public void reset() {
        la.reset();
        // al.set(0);
    }

    public void inc() {
        la.increment();
        //al.incrementAndGet();
    }

    public void dec() {
        la.decrement();
        //al.decrementAndGet();
    }

    public void add(long val) {
        la.add(val);
        // al.addAndGet(val);
    }

    public long get() {
        return la.sum();
        //return al.get();
    }

    public long getAndReset() {
        return la.sumThenReset();
        //return al.get();
    }
}
