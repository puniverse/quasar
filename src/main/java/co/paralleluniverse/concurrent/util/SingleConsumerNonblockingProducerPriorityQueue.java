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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author pron
 */
class SingleConsumerNonblockingProducerPriorityQueue<E> implements BlockingQueue<E> {
    private final ConcurrentSkipListSet<E> sls;
    //
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition available = lock.newCondition();
    private final AtomicBoolean consumerBlocking = new AtomicBoolean();
//    private final Semaphore available = new Semaphore(0);
    
    public SingleConsumerNonblockingProducerPriorityQueue() {
        this.sls = new ConcurrentSkipListSet<E>();
    }

    public SingleConsumerNonblockingProducerPriorityQueue(Comparator<? super E> comparator) {
        this.sls = new ConcurrentSkipListSet<E>(comparator);
    }

    @Override
    public boolean add(E e) {
        final boolean res = sls.add(e);
//        if(res)
//            available.release();
        if (consumerBlocking.compareAndSet(true, false)) { // if the consumer is not blocking, then it MUST check the queue again
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
    public E poll() {
        return sls.pollFirst();
    }

    @Override
    public E take() throws InterruptedException {
        E e = sls.pollFirst();
        if (e == null) {
            consumerBlocking.set(true);
            lock.lock();
            try {
                e = sls.pollFirst();
                while (e == null || !isValid(e)) {
                    available.await();
                    e = sls.pollFirst();
                }
            } finally {
                consumerBlocking.set(false);
                lock.unlock();
            }
        }
        return e;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = sls.pollFirst();

        if (e == null) {
            consumerBlocking.set(true);
            long left = unit.toNanos(timeout);
            lock.lock();
            try {
                e = sls.pollFirst();
                while (e == null || !isValid(e)) {
                    left = available.awaitNanos(left);
                    e = sls.pollFirst();
                    if (left < 0)
                        return e;
                }
            } finally {
                consumerBlocking.set(false);
                lock.unlock();
            }
        }
        return e;
    }

    protected boolean isValid(E e) {
        return true;
    }

    //////////// Boring //////////////////////////
    @Override
    public boolean offer(E e) {
        add(e);
        return true;
    }

    @Override
    public void put(E e) {
        add(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        add(e);
        return true;
    }

    @Override
    public E remove() {
        E e = sls.pollFirst();
        if (e == null)
            throw new NoSuchElementException();
        return e;
    }

    @Override
    public E element() {
        E e = sls.first();
        if (e == null)
            throw new NoSuchElementException();
        return e;
    }

    @Override
    public E peek() {
        if(sls.isEmpty())
            return null; 
        return sls.first(); // remember, we're single consumer. If we weren't empty we stay non-empty as no one else can take elements.
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        int count = 0;
        E e = sls.pollFirst();
        while (e != null) {
            c.add(e);
            count++;
            e = sls.pollFirst();
        }
        return count;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        int count = 0;
        E e = null;
        if (count < maxElements)
            e = sls.pollFirst();
        while (e != null && count < maxElements) {
            c.add(e);
            count++;
            if (count < maxElements)
                e = sls.pollFirst();
        }
        return count;
    }

    //////////// Simple delegates ////////////////
    @Override
    public String toString() {
        return sls.toString();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return sls.toArray(a);
    }

    @Override
    public Iterator<E> iterator() {
        return sls.iterator();
    }

    @Override
    public int size() {
        return sls.size();
    }

    @Override
    public Object[] toArray() {
        return sls.toArray();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return sls.retainAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return sls.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return sls.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return sls.removeAll(c);
    }

    @Override
    public boolean remove(Object o) {
        return sls.remove(o);
    }

    @Override
    public boolean isEmpty() {
        return sls.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return sls.contains(o);
    }

    @Override
    public void clear() {
        sls.clear();
    }
}
