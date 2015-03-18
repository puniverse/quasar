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
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayFloatQueue extends SingleConsumerArrayWordQueue<Float> implements BasicSingleConsumerFloatQueue {
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

    float floatValue(int index) {
        return Float.intBitsToFloat(rawValue(index));
    }

    @Override
    Float value(int index) {
        return floatValue(index);
    }

    @Override
    public float pollFloat() {
        final int n = pk();
        final float val = floatValue(n);
        deq(n);
        return val;
    }

    @Override
    public FloatQueueIterator iterator() {
        return new FloatArrayQueueIterator();
    }

    private class FloatArrayQueueIterator extends ArrayQueueIterator implements FloatQueueIterator {

        @Override
        public float floatValue() {
            return SingleConsumerArrayFloatQueue.this.floatValue(n);
        }

        @Override
        public float floatNext() {
            n = succ(n);
            return floatValue();
        }
    }
}
