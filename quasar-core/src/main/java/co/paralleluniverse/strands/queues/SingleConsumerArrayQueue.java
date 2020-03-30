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

import com.google.common.collect.Lists;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pron
 */
abstract class SingleConsumerArrayQueue<E> extends SingleConsumerQueue<E> {
    final int capacity;
    final int mask;
    volatile int p001, p002, p003, p004, p005, p006, p007;
    volatile long head; // next element to be read
    volatile long p101, p102, p103, p104, p105, p106, p107;
    volatile long tail; // next element to be written
    volatile long p201, p202, p203, p204, p205, p206, p207;
    int seed = (int) System.nanoTime();

    SingleConsumerArrayQueue(int capacity) {
        // size is a power of 2
        this.capacity = nextPowerOfTwo(capacity);
        this.mask = this.capacity - 1;
    }

    private static int nextPowerOfTwo(int v) {
        assert v >= 0;
        return 1 << (32 - Integer.numberOfLeadingZeros(v - 1));
    }

    @Override
    public int capacity() {
        return capacity;
    }

    abstract E value(int index);

    abstract int arrayLength();

    abstract void awaitValue(long index);

    abstract void clearValue(int index);

    abstract void copyValue(int to, int from);

    long maxReadIndex() {
        return tail;
    }

    final long preEnq() {
        long t, w;
        for (;;) {
            t = tail;
            w = t - capacity; // "wrap point"
            if (head <= w)
                return -1;

            if (compareAndSetTail(t, t + 1))
                break;
            backoff();
        }
        return t;
    }

    public void deq(int index) {
        assert index == (head & mask);
        clearValue(index);
        orderedSetHead(head + 1); // head = newHead; // 
    }

    abstract boolean hasNext(long lind, int iind);

    @Override
    public boolean hasNext() {
        final long h = head;
        return hasNext(h, (int) h & mask);
    }

    int pk() {
        final long h = head;
        final int h1 = (int) head & mask;
        if (!hasNext(h, h1))
            return -1;
        return h1;
    }

    @Override
    public E poll() {
        final int i = pk();
        if (i < 0)
            return null;
        final E v = value(i);
        deq(i);
        return v;
    }

    @Override
    public E peek() {
        final int i = pk();
        return i >= 0 ? value(i) : null;
    }

    @SuppressWarnings("empty-statement")
    int succ(int index) {
        if (index < 0)
            return pk();
        int n1 = (index + 1) & mask;
        long n = intToLongIndex(n1);
        if (!hasNext(n, n1))
            return -1;
        return (int) n & mask;
    }

    int del(int index) {
        if (index == ((int) head & mask)) {
            deq(index);
            return -1;
        }

        clearValue(index);
        long i = intToLongIndex(index);
        long t = tail;
        if (i == t) {
            if (compareAndSetTail(t, t - 1))
                return (int) (i - 1) & mask;
        }

        final long h = head;
        for (; i != h; i--)
            copyValue((int) i & mask, (int) (i - 1) & mask);
        clearValue((int) (h & mask));
        head = h + 1; // orderedSetHead(h + 1); // 
        return index;
    }

    private long intToLongIndex(int index) {
        final int ih = (int) head & mask;
        return head + (index >= ih ? index - ih : index + capacity - ih);
    }

    @Override
    public int size() {
        return (int) (tail - head);
    }

    @Override
    public boolean isEmpty() {
        return tail == head;
    }

    @Override
    public List<E> snapshot() {
        final long t = tail;
        final ArrayList<E> list = new ArrayList<E>((int) (t - head));
        for (long p = tail; p != head; p--)
            list.add(value((int) p & mask));

        return Lists.reverse(list);
    }

    int next(int i) {
        return (i + 1) & mask;
    }

    int prev(int i) {
        return --i & mask;
    }

    void backoff() {
        int spins = 1 << 8;
        int r = seed;
        while (spins >= 0) {
            r ^= r << 1;
            r ^= r >>> 3;
            r ^= r << 10; // xorshift
            if (r >= 0)
                --spins;
        }
        seed = r;
    }

    @Override
    public QueueIterator<E> iterator() {
        return new ArrayQueueIterator();
    }

    class ArrayQueueIterator implements QueueIterator<E> {
        int n = -1;

        @Override
        public boolean hasNext() {
            return succ(n) >= 0;
        }

        @Override
        public E value() {
            return SingleConsumerArrayQueue.this.value(n);
        }

        @Override
        public void deq() {
            SingleConsumerArrayQueue.this.deq(n);
        }

        @Override
        public void reset() {
            n = -1;
        }

        @Override
        public E next() {
            n = succ(n);
            return value();
        }

        @Override
        public void remove() {
            n = del(n);
        }
    }
    ////////////////////////////////////////////////////////////////////////
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(SingleConsumerArrayQueue.class, "head", long.class);
            TAIL = l.findVarHandle(SingleConsumerArrayQueue.class, "tail", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * CAS tail field. Used only by preEnq.
     */
    private boolean compareAndSetTail(long expect, long update) {
        return TAIL.compareAndSet(this, expect, update);
    }

    private void orderedSetHead(long value) {
        HEAD.setOpaque(this, value); // UNSAFE.putOrderedLong(this, headOffset, value);
    }
}
