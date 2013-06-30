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
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayDoubleQueue extends SingleConsumerArrayDWordQueue<Double> implements SingleConsumerDoubleQueue<Integer> {
    public SingleConsumerArrayDoubleQueue(int capacity) {
        super(capacity);
    }

    @Override
    public boolean enq(double item) {
        return enqRaw(Double.doubleToRawLongBits(item));
    }

    @Override
    public boolean enq(Double item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        return enq(item.doubleValue());
    }

    public double doubleValue(int index) {
        return Double.longBitsToDouble(rawValue(index));
    }

    @Override
    public Double value(int index) {
        return doubleValue(index);
    }

    @Override
    public double doubleValue(Integer node) {
        return doubleValue(node.intValue());
    }
}
