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

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author pron
 */
public class SingleConsumerNonblockingProducerPriorityQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    final ConcurrentSkipListPriorityQueue<E> pq;
    //
    final ReentrantLock lock = new ReentrantLock();
    final Condition available = lock.newCondition();
    volatile boolean consumerBlocking;
//    private final Semaphore available = new Semaphore(0);

    public SingleConsumerNonblockingProducerPriorityQueue() {
        this.pq = new ConcurrentSkipListPriorityQueue<E>();
    }

    public SingleConsumerNonblockingProducerPriorityQueue(Comparator<? super E> comparator) {
        this.pq = new ConcurrentSkipListPriorityQueue<E>(comparator);
    }

    @Override
    public boolean offer(E e) {
        final boolean res = pq.offer(e);
//        if(res)
//            available.release();
        if (consumerBlocking && pq.peek() == e) { // if the consumer is not blocking, then it MUST check the queue again
            lock.lock();
            try {
                available.signal();
            } finally {
                lock.unlock();
            }
        }
        return res;
    }

    @Override
    public E take() throws InterruptedException {
        E e = pq.poll();
        if (e == null) {
            consumerBlocking = true;
            lock.lock();
            try {
                e = pq.poll();
                while (e == null) {
                    available.await();
                    e = pq.poll();
                }
            } finally {
                consumerBlocking = false;
                lock.unlock();
            }
        }
        return e;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = pq.poll();
        if (e == null) {
            consumerBlocking = true;
            long left = unit.toNanos(timeout);
            lock.lock();
            try {
                e = pq.poll();
                while (e == null) {
                    left = available.awaitNanos(left);
                    if (left < 0)
                        return null;
                    e = pq.poll();
                }
            } finally {
                consumerBlocking = false;
                lock.unlock();
            }
        }
        return e;
    }

    //////////// Boring //////////////////////////
    @Override
    public void put(E e) {
        add(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }

    @Override
    public E poll() {
        return pq.poll();
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        int count = 0;
        E e = pq.poll();
        while (e != null) {
            c.add(e);
            count++;
            e = pq.poll();
        }
        return count;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        int count = 0;
        E e = null;
        if (count < maxElements)
            e = pq.poll();
        while (e != null && count < maxElements) {
            c.add(e);
            count++;
            if (count < maxElements)
                e = pq.poll();
        }
        return count;
    }

    //////////// Simple delegates ////////////////
    @Override
    public E peek() {
        return pq.peek();
    }

    @Override
    public String toString() {
        return pq.toString();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return pq.toArray(a);
    }

    @Override
    public Iterator<E> iterator() {
        return pq.iterator();
    }

    @Override
    public int size() {
        return pq.size();
    }

    @Override
    public Object[] toArray() {
        return pq.toArray();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return pq.retainAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return pq.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return pq.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return pq.removeAll(c);
    }

    @Override
    public boolean remove(Object o) {
        return pq.remove(o);
    }

    @Override
    public boolean isEmpty() {
        return pq.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return pq.contains(o);
    }

    @Override
    public void clear() {
        pq.clear();
    }
}
