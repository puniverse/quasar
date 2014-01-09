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
public class SingleConsumerLinkedLongQueue extends SingleConsumerLinkedDWordQueue<Long>
        implements SingleConsumerLongQueue<SingleConsumerLinkedQueue.Node<Long>>, BasicSingleConsumerLongQueue {
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

    @Override
    public long longValue(Node<Long> node) {
        return rawValue(node);
    }

    @Override
    public Long value(Node<Long> node) {
        return longValue(node);
    }

    @Override
    public long pollLong() {
        final Node n = pk();
        final long val = longValue(n);
        deq(n);
        return val;
    }
}
