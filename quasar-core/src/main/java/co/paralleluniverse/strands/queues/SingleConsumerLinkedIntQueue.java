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
public class SingleConsumerLinkedIntQueue extends SingleConsumerLinkedWordQueue<Integer>
        implements SingleConsumerIntQueue<SingleConsumerLinkedQueue.Node<Integer>>, BasicSingleConsumerIntQueue {
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

    @Override
    public int intValue(Node<Integer> node) {
        return rawValue(node);
    }

    @Override
    public Integer value(Node<Integer> node) {
        return intValue(node);
    }

    @Override
    public int pollInt() {
        final Node n = pk();
        final int val = intValue(n);
        deq(n);
        return val;
    }
}
