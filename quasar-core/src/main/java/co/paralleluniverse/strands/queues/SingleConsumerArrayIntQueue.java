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
public class SingleConsumerArrayIntQueue extends SingleConsumerArrayWordQueue<Integer> implements BasicSingleConsumerIntQueue {
    public SingleConsumerArrayIntQueue(int capcity) {
        super(capcity);
    }

    @Override
    public boolean enq(int item) {
        return super.enqRaw(item);
    }

    @Override
    public boolean enq(Integer item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        return enq(item.intValue());
    }

    public int intValue(int index) {
        return rawValue(index);
    }

    @Override
    Integer value(int index) {
        return intValue(index);
    }

    @Override
    public int pollInt() {
        final int n = pk();
        final int val = intValue(n);
        deq(n);
        return val;
    }

    @Override
    public IntQueueIterator iterator() {
        return new IntArrayQueueIterator();
    }

    private class IntArrayQueueIterator extends ArrayQueueIterator implements IntQueueIterator {

        @Override
        public int intValue() {
            return SingleConsumerArrayIntQueue.this.intValue(n);
        }

        @Override
        public int intNext() {
            n = succ(n);
            return intValue();
        }
    }
}
