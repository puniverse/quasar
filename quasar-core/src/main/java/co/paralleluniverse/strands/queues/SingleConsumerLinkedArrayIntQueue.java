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
public class SingleConsumerLinkedArrayIntQueue extends SingleConsumerLinkedArrayWordQueue<Integer> implements BasicSingleConsumerIntQueue {
    @Override
    public boolean enq(int element) {
        return enqRaw(element);
    }

    @Override
    public boolean enq(Integer element) {
        return enq(element.intValue());
    }

    @Override
    Integer value(Node n, int i) {
        return intValue(n, i);
    }

    int intValue(Node n, int i) {
        return rawValue(n, i);
    }

    @Override
    public int pollInt() {
        return (int)pollRaw();
    }

    @Override
    public IntQueueIterator iterator() {
        return new IntLinkedArrayQueueIterator();
    }

    private class IntLinkedArrayQueueIterator extends LinkedArrayQueueIterator implements IntQueueIterator {
        @Override
        public int intValue() {
            return SingleConsumerLinkedArrayIntQueue.this.intValue(n, i);
        }

        @Override
        public int intNext() {
            preNext();
            return intValue();
        }
    }
}
