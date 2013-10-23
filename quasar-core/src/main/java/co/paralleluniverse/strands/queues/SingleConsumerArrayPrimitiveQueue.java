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
abstract class SingleConsumerArrayPrimitiveQueue<E> extends SingleConsumerArrayQueue<E> {
    private volatile Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    volatile long maxReadIndex;

    public SingleConsumerArrayPrimitiveQueue(int capacity) {
        super(capacity);
    }
    
    @Override
    long maxReadIndex() {
        return maxReadIndex;
    }

    @Override
    void clearValue(long index) {
    }

    @Override
    boolean hasNext(long lind, int iind) {
        return lind < maxReadIndex;
    }

    @SuppressWarnings("empty-statement")
    @Override
    void awaitValue(long i) {
        while (maxReadIndex < i)
            ;
    }

    @SuppressWarnings("empty-statement")
    final void postEnq(long i) {
        if (true) {
            while (maxReadIndex != i)
            ;
            maxReadIndex = i + 1;
        } else {
            while (!compareAndSetMaxReadIndex(i, i + 1))
            ;
        }
    }
    private static final long maxReadIndexOffset;

    static {
        try {
            maxReadIndexOffset = UNSAFE.objectFieldOffset(SingleConsumerArrayPrimitiveQueue.class.getDeclaredField("maxReadIndex"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    /**
     * CAS maxReadIndex field. Used only by postEnq.
     */
    private boolean compareAndSetMaxReadIndex(long expect, long update) {
        return UNSAFE.compareAndSwapLong(this, maxReadIndexOffset, expect, update);
    }
}
