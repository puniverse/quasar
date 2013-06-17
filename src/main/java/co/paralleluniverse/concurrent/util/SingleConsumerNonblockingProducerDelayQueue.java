/*
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
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
class SingleConsumerNonblockingProducerDelayQueue<E extends Delayed> extends SingleConsumerNonblockingProducerPriorityQueue<E> {
    public SingleConsumerNonblockingProducerDelayQueue() {
    }

    @Override
    public E poll() {
        final E first = peek();
        if (first == null || !isValid(first))
            return null;
        return super.poll(); // this could be a different element from first, but its expiration must be earlier, so it doesn't matter
    }

    @Override
    protected boolean isValid(E e) {
        return e.getDelay(TimeUnit.NANOSECONDS) <= 0;
    }
}
