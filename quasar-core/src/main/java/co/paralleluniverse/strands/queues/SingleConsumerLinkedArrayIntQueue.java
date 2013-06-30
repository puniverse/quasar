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
public class SingleConsumerLinkedArrayIntQueue extends SingleConsumerLinkedArrayWordQueue<Integer> implements SingleConsumerIntQueue<SingleConsumerLinkedArrayQueue.ElementPointer> {

    @Override
    public boolean enq(int element) {
        return enqRaw(element);
    }

    @Override
    public boolean enq(Integer element) {
        return enq(element.intValue());
    }

    @Override
    public Integer value(ElementPointer node) {
        return intValue(node);
    }

    @Override
    public int intValue(ElementPointer node) {
        return rawValue(node.n, node.i);
    }
}
