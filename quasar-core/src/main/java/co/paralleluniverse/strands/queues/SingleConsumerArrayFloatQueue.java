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
public class SingleConsumerArrayFloatQueue extends SingleConsumerArrayWordQueue<Float> implements SingleConsumerFloatQueue<Integer> {
    public SingleConsumerArrayFloatQueue(int capacity) {
        super(capacity);
    }

    @Override
    public boolean enq(float item) {
        return enqRaw(Float.floatToRawIntBits(item));
    }

    @Override
    public boolean enq(Float item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        return enq(item.floatValue());
    }

    public float floatValue(int index) {
        return Float.intBitsToFloat(rawValue(index));
    }

    @Override
    public Float value(int index) {
        return floatValue(index);
    }

    @Override
    public float floatValue(Integer node) {
        return floatValue(node.intValue());
    }
}
