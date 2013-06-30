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

import co.paralleluniverse.concurrent.util.UtilUnsafe;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
public class ArrayQueue<E> implements BasicQueue<E> {
    final int capacity;
    final int mask;
    volatile int p001, p002, p003, p004, p005, p006, p007;
    volatile long head; // next element to be read
    volatile long p101, p102, p103, p104, p105, p106, p107;
    volatile long tail; // next element to be written
    volatile long p201, p202, p203, p204, p205, p206, p207;
    private long cachedHead;
    volatile long p301, p302, p303, p304, p305, p306, p307;
    private long cachedTail;
    volatile Object p401, p402, p403, p404, p405, p406, p407;
    private final Object[] array;

    public ArrayQueue(int capacity) {
        // size is a power of 2
        this.capacity = nextPowerOfTwo(capacity);
        this.mask = this.capacity - 1;
        this.array = new Object[this.capacity];
    }

    private static int nextPowerOfTwo(int v) {
        assert v >= 0;
        return 1 << (32 - Integer.numberOfLeadingZeros(v - 1));
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean enq(E item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        final long i = preEnq();
        if (i < 0)
            return false;
        set((int) i & mask, item);
        return true;
    }

    private long preEnq() {
        long t, w;
        do {
            t = tail;
            w = t - capacity; // "wrap point"

            if (cachedHead <= w) {
                cachedHead = head; // only time a producer reads head. for this, head needs to be volatile. can we do better?
                if (cachedHead <= w)
                    return -1;
            }
        } while (!compareAndSetTail(t, t + 1));
        return t;
    }

    @Override
    public E poll() {
        long h;
        E v;
        do {
            h = head;
            if (h >= cachedTail) {
                cachedTail = tail;
                if (h >= cachedTail)
                    return null;
            }

            while ((v = get((int) h & mask)) == null); // volatile read
        } while (!compareAndSetHead(h, h + 1));
        cas((int) h & mask, v, null);
        return v;
    }

    @Override
    public int size() {
        return (int) (tail - head);
    }

    int next(int i) {
        return (i + 1) & mask;
    }

    int prev(int i) {
        return --i & mask;
    }
    ////////////////////////////////////////////////////////////////////////
    static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final int base;
    private static final int shift;
    private static final long headOffset;
    private static final long tailOffset;

    static {
        try {
            headOffset = unsafe.objectFieldOffset(ArrayQueue.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(ArrayQueue.class.getDeclaredField("tail"));

            base = unsafe.arrayBaseOffset(Object[].class);
            int scale = unsafe.arrayIndexScale(Object[].class);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            shift = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    /**
     * CAS tail field. Used only by preEnq.
     */
    private boolean compareAndSetTail(long expect, long update) {
        return unsafe.compareAndSwapLong(this, tailOffset, expect, update);
    }

    private boolean compareAndSetHead(long expect, long update) {
        return unsafe.compareAndSwapLong(this, headOffset, expect, update);
    }

    private void orderedSetHead(long value) {
        unsafe.putOrderedLong(this, headOffset, value);
    }

    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }

    private void set(int i, E value) {
        unsafe.putObjectVolatile(array, byteOffset(i), value);
    }

    private void orderedSet(int i, E value) {
        unsafe.putOrderedObject(array, byteOffset(i), value);
    }

    private E get(int i) {
        return (E) unsafe.getObjectVolatile(array, byteOffset(i));
    }

    private boolean cas(int i, E expected, E update) {
        return unsafe.compareAndSwapObject(array, byteOffset(i), expected, update);
    }
}
