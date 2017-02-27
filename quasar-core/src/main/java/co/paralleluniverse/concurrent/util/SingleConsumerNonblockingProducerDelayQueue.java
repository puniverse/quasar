/*
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
package co.paralleluniverse.concurrent.util;

import java.util.Queue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class SingleConsumerNonblockingProducerDelayQueue<E extends Delayed> extends SingleConsumerNonblockingProducerQueue<E> {
    public SingleConsumerNonblockingProducerDelayQueue() {
        super(new ConcurrentSkipListPriorityQueue<E>());
    }
    
    public SingleConsumerNonblockingProducerDelayQueue(Queue<E> q) {
        super(q);
    }

    @Override
    public E poll() {
        final E first = peek();
        if (first == null || !isExpired(first))
            return null;
        return super.poll(); // this could be a different element from first, but its expiration must be earlier, so it doesn't matter
    }

    @Override
    public E take() throws InterruptedException {
        E e = q.peek();
        long delayNanos;
        if (e == null || getDelay(e) > 0) {
            sync.register();
            try {
                e = q.peek();
                delayNanos = getDelay(e);

                while (delayNanos > 0) {
                    sync.awaitNanos(delayNanos);
                    e = q.peek();
                    delayNanos = getDelay(e);
                }
                return q.poll(); // this may not be e, but if not, it must be an element with expiration <= e.
            } finally {
                sync.unregister();
            }
        } else {
            e = q.poll();
            assert e != null;
            return e;
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = q.peek();
        long delayNanos;
        if (e == null || getDelay(e) > 0) {
            long left = unit.toNanos(timeout);
            sync.register();
            try {
                e = q.peek();
                delayNanos = getDelay(e);

                while (left > 0 & delayNanos > 0) {
                    left = sync.awaitNanos(Math.min(left, delayNanos));
                    e = q.peek();
                    delayNanos = getDelay(e);
                }
                return delayNanos > 0 ? null : q.poll();
            } finally {
                sync.unregister();
            }
        } else {
            e = q.poll();
            assert e != null;
            return e;
        }
    }

    private long getDelay(E e) {
        return e != null ? e.getDelay(TimeUnit.NANOSECONDS) : Long.MAX_VALUE;
    }

    protected boolean isExpired(E e) {
        return e.getDelay(TimeUnit.NANOSECONDS) <= 0;
    }
}
