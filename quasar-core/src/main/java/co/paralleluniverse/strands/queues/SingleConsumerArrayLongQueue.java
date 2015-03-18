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
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayLongQueue extends SingleConsumerArrayDWordQueue<Long> implements BasicSingleConsumerLongQueue {
    public SingleConsumerArrayLongQueue(int capacity) {
        super(capacity);
    }

    @Override
    public boolean enq(long item) {
        return enqRaw(item);
    }

    @Override
    public boolean enq(Long item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        return enq(item.longValue());
    }

    long longValue(int index) {
        return rawValue(index);
    }

    @Override
    Long value(int index) {
        return longValue(index);
    }

    @Override
    public long pollLong() {
        final int n = pk();
        final long val = longValue(n);
        deq(n);
        return val;
    }

    @Override
    public LongQueueIterator iterator() {
        return new LongArrayQueueIterator();
    }

    private class LongArrayQueueIterator extends ArrayQueueIterator implements LongQueueIterator {

        @Override
        public long longValue() {
            return SingleConsumerArrayLongQueue.this.longValue(n);
        }

        @Override
        public long longNext() {
            n = succ(n);
            return longValue();
        }
    }
}
