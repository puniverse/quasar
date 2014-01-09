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

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class SingleConsumerNonblockingProducerQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    final Queue<E> q;
    //
    final OwnedSynchronizer sync = new OwnedSynchronizer2();

    public SingleConsumerNonblockingProducerQueue(Queue<E> q) {
        this.q = q;
    }

    @Override
    public boolean offer(E e) {
        final boolean res = q.offer(e);
//        if(res)
//            available.release();
        if (sync.shouldSignal() && q.peek() == e) // if the consumer is not blocking, then it MUST check the queue again
            sync.signal();
        return res;
    }

    @Override
    public E take() throws InterruptedException {
        E e = q.poll();
        if (e == null) {
            sync.register();
            try {
                e = q.poll();
                while (e == null) {
                    sync.await();
                    e = q.poll();
                }
            } finally {
                sync.unregister();
            }
        }
        return e;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = q.poll();
        if (e == null) {
            long left = unit.toNanos(timeout);
            sync.register();
            try {
                e = q.poll();
                while (e == null) {
                    left = sync.awaitNanos(left);
                    if (left < 0)
                        return null;
                    e = q.poll();
                }
            } finally {
                sync.unregister();
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
        return q.poll();
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        int count = 0;
        E e = q.poll();
        while (e != null) {
            c.add(e);
            count++;
            e = q.poll();
        }
        return count;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        int count = 0;
        E e = null;
        if (count < maxElements)
            e = q.poll();
        while (e != null && count < maxElements) {
            c.add(e);
            count++;
            if (count < maxElements)
                e = q.poll();
        }
        return count;
    }

    //////////// Simple delegates ////////////////
    @Override
    public E peek() {
        return q.peek();
    }

    @Override
    public String toString() {
        return q.toString();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return q.toArray(a);
    }

    @Override
    public Iterator<E> iterator() {
        return q.iterator();
    }

    @Override
    public int size() {
        return q.size();
    }

    @Override
    public Object[] toArray() {
        return q.toArray();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return q.retainAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return q.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return q.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return q.removeAll(c);
    }

    @Override
    public boolean remove(Object o) {
        return q.remove(o);
    }

    @Override
    public boolean isEmpty() {
        return q.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return q.contains(o);
    }

    @Override
    public void clear() {
        q.clear();
    }
}
