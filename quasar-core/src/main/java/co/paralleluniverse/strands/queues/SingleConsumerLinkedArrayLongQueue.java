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
public class SingleConsumerLinkedArrayLongQueue extends SingleConsumerLinkedArrayDWordQueue<Long>
        implements SingleConsumerLongQueue<SingleConsumerLinkedArrayQueue.ElementPointer>, BasicSingleConsumerLongQueue {
    @Override
    public boolean enq(long element) {
        return enqRaw(element);
    }

    @Override
    public boolean enq(Long element) {
        return enq(element.longValue());
    }

    @Override
    Long value(Node n, int i) {
        return longValue(n, i);
    }

    @Override
    public long longValue(ElementPointer node) {
        return longValue(node.n, node.i);
    }

    private long longValue(Node n, int i) {
        return rawValue(n ,i);
    }

    @Override
    public long pollLong() {
        final ElementPointer n = pk();
        final long val = longValue(n);
        deq(n);
        return val;
    }
}
