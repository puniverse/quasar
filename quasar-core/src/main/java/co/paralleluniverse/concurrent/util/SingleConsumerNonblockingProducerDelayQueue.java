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
        if (first == null || !isExpired(first))
            return null;
        return super.poll(); // this could be a different element from first, but its expiration must be earlier, so it doesn't matter
    }

    @Override
    public E take() throws InterruptedException {
        E e = sls.peekFirst();
        long delayNanos;
        if (e == null || getDelay(e) > 0) {
            consumerBlocking = true;
            lock.lock();
            try {
                e = sls.peekFirst();
                delayNanos = getDelay(e);

                while (delayNanos > 0) {
                    available.awaitNanos(delayNanos);
                    e = sls.peekFirst();
                    delayNanos = getDelay(e);
                }
                return sls.pollFirst(); // this may not be e, but if not, it must be an element with expiration <= e.
            } finally {
                consumerBlocking = false;
                lock.unlock();
            }
        } else {
            e = sls.pollFirst();
            assert e != null;
            return e;
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = sls.peekFirst();
        long delayNanos;
        if (e == null || getDelay(e) > 0) {
            consumerBlocking = true;
            long left = unit.toNanos(timeout);
            lock.lock();
            try {
                e = sls.peekFirst();
                delayNanos = getDelay(e);

                while (left > 0 & delayNanos > 0) {
                    left = available.awaitNanos(Math.min(left, delayNanos));
                    e = sls.peekFirst();
                    delayNanos = getDelay(e);
                }
                return delayNanos > 0 ? null : sls.pollFirst();
            } finally {
                consumerBlocking = false;
                lock.unlock();
            }
        } else {
            e = sls.pollFirst();
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
