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
abstract class CircularWordBuffer<E> extends CircularBuffer<E> {
    private final int[] array;

    public CircularWordBuffer(int size, boolean singleProducer) {
        super(size, singleProducer);
        this.array = new int[capacity];
    }

    void enqRaw(int elem) {
        long index = preEnq();
        orderedSet((int) index & mask, elem); // must be orderedSet so as to not be re-ordered with tail bump in postEnq
        postEnq();
    }

    abstract class WordConsumer extends Consumer {
        private int value;

        @Override
        protected void grabValue(int index) {
            value = array[index];
        }

        @Override
        protected void clearValue() {
        }

        int getRawValue() {
            return value;
        }
    }
    //////////////////////////
    private static final int base;
    private static final int shift;

    static {
        try {
            base = unsafe.arrayBaseOffset(int[].class);
            int scale = unsafe.arrayIndexScale(int[].class);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            shift = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }

    private void orderedSet(int i, int value) {
        unsafe.putOrderedInt(array, byteOffset(i), value);
    }
}
