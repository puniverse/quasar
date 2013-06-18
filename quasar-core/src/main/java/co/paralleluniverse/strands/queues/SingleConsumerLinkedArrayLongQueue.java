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
public class SingleConsumerLinkedArrayLongQueue extends SingleConsumerLinkedArrayDWordQueue<Long> implements SingleConsumerLongQueue<SingleConsumerLinkedArrayQueue.ElementPointer> {

    @Override
    public boolean enq(long element) {
        return super.enq(element);
    }

    @Override
    public boolean enq(Long element) {
        return enq(element.longValue());
    }

    @Override
    public Long value(ElementPointer node) {
        return longValue(node);
    }

    @Override
    public long longValue(ElementPointer node) {
        return rawValue(node.n, node.i);
    }
}
