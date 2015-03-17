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
public class SingleConsumerLinkedIntQueue extends SingleConsumerLinkedWordQueue<Integer> implements BasicSingleConsumerIntQueue {
    @Override
    public boolean enq(int item) {
        return enqRaw(item);
    }

    @Override
    public boolean enq(Integer item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        return enq(item.intValue());
    }

    int intValue(Node<Integer> node) {
        return rawValue(node);
    }

    @Override
    Integer value(Node<Integer> node) {
        return intValue(node);
    }

    @Override
    public int pollInt() {
        final Node n = pk();
        final int val = intValue(n);
        deq(n);
        return val;
    }

    @Override
    public IntQueueIterator iterator() {
        return new IntLinkedQueueIterator();
    }

    private class IntLinkedQueueIterator extends LinkedQueueIterator implements IntQueueIterator {
        @Override
        public int intValue() {
            return SingleConsumerLinkedIntQueue.this.intValue(n);
        }

        @Override
        public int intNext() {
            n = succ(n);
            return intValue();
        }
    }
}
