/*
 * Copyright (c) 2011-2014, Parallel Universe Software Co. All rights reserved.
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
import jsr166e.LongAdder;

/**
 *
 * @author pron
 */
public class Counter extends LongAdder {
//    private final AtomicLong al = new AtomicLong();

//    public void reset() {
//        la.reset();
//    }
    
    public void inc() {
        increment();
        //al.incrementAndGet();
    }

    public void dec() {
        decrement();
        //al.decrementAndGet();
    }

//    public void add(long val) {
//        al.addAndGet(val);
//    }
    
    public long get() {
        return sum();
        //return al.get();
    }

    public long getAndReset() {
        return sumThenReset();
        //return al.get();
    }
}
