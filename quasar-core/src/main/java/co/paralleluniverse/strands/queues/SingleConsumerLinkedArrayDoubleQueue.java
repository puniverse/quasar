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
public class SingleConsumerLinkedArrayDoubleQueue extends SingleConsumerLinkedArrayDWordQueue<Double> 
implements SingleConsumerDoubleQueue<SingleConsumerLinkedArrayQueue.ElementPointer>, BasicSingleConsumerDoubleQueue {

    @Override
    public boolean enq(double element) {
        return enqRaw(Double.doubleToRawLongBits(element));
    }

    @Override
    public boolean enq(Double element) {
        return enq(element.doubleValue());
    }

    @Override
    Double value(Node n, int i) {
        return doubleValue(n, i);
    }

    @Override
    public double doubleValue(ElementPointer node) {
        return doubleValue(node.n, node.i);
    }
    
    private double doubleValue(Node n, int i) {
        return Double.longBitsToDouble(rawValue(n, i));
    }
    
    @Override
    public double pollDouble() {
        final ElementPointer n = pk();
        final double val = doubleValue(n);
        deq(n);
        return val;
    }
}
