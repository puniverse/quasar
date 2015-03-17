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
public class SingleConsumerLinkedDoubleQueue extends SingleConsumerLinkedDWordQueue<Double> implements BasicSingleConsumerDoubleQueue {
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

    double doubleValue(Node<Double> node) {
        return Double.longBitsToDouble(rawValue(node));
    }

    @Override
    Double value(Node<Double> node) {
        return doubleValue(node);
    }

    @Override
    public double pollDouble() {
        final Node n = pk();
        final double val = doubleValue(n);
        deq(n);
        return val;
    }

    @Override
    public DoubleQueueIterator iterator() {
        return new DoubleLinkedQueueIterator();
    }

    private class DoubleLinkedQueueIterator extends LinkedQueueIterator implements DoubleQueueIterator {
        @Override
        public double doubleValue() {
            return SingleConsumerLinkedDoubleQueue.this.doubleValue(n);
        }

        @Override
        public double doubleNext() {
            n = succ(n);
            return doubleValue();
        }
    }
}
