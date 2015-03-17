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
public class SingleConsumerLinkedArrayFloatQueue extends SingleConsumerLinkedArrayWordQueue<Float> implements BasicSingleConsumerFloatQueue {
    @Override
    public boolean enq(float element) {
        return enqRaw(Float.floatToRawIntBits(element));
    }

    @Override
    public boolean enq(Float element) {
        return enq(element.floatValue());
    }

    @Override
    Float value(Node n, int i) {
        return floatValue(n, i);
    }

    float floatValue(Node n, int i) {
        return Float.intBitsToFloat(rawValue(n, i));
    }

    @Override
    public float pollFloat() {
        return Float.intBitsToFloat((int) pollRaw());
    }

    @Override
    public FloatQueueIterator iterator() {
        return new FloatLinkedArrayQueueIterator();
    }

    private class FloatLinkedArrayQueueIterator extends LinkedArrayQueueIterator implements FloatQueueIterator {
        @Override
        public float floatValue() {
            return SingleConsumerLinkedArrayFloatQueue.this.floatValue(n, i);
        }

        @Override
        public float floatNext() {
            preNext();
            return floatValue();
        }
    }
}
