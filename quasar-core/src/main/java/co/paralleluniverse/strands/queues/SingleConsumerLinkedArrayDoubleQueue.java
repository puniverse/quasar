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
public class SingleConsumerLinkedArrayDoubleQueue extends SingleConsumerLinkedArrayDWordQueue<Double> implements SingleConsumerDoubleQueue<SingleConsumerLinkedArrayQueue.ElementPointer> {

    @Override
    public boolean enq(double element) {
        return super.enq(Double.doubleToRawLongBits(element));
    }

    @Override
    public boolean enq(Double element) {
        return enq(element.doubleValue());
    }

    @Override
    public Double value(ElementPointer node) {
        return doubleValue(node);
    }

    @Override
    public double doubleValue(ElementPointer node) {
        return Double.longBitsToDouble(rawValue(node.n, node.i));
    }
}
