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
public class SingleProducerCircularObjectBuffer<E> extends SingleProducerCircularBuffer<E> {
    private final Object[] array;

    public SingleProducerCircularObjectBuffer(int size, boolean singleProducer) {
        super(size, singleProducer);
        this.array = new Object[capacity];
    }

    @Override
    public void enq(E elem) {
        long index = preEnq();
        orderedSet((int) index & mask, elem); // must be orderedSet so as to not be re-ordered with tail bump in postEnq
        postEnq();
    }

    @Override
    public Consumer<E> newConsumer() {
        return new ObjectConsumer<E>();
    }

    private class ObjectConsumer<E> extends Consumer<E> {
        private Object value;

        @Override
        protected void grabValue(int index) {
            value = array[index];
        }

        @Override
        protected void clearValue() {
            value = null;
        }

        @Override
        protected E getValue() {
            return (E) value;
        }
    }
    //////////////////////////
    private static final int base;
    private static final int shift;

    static {
        try {
            base = unsafe.arrayBaseOffset(Object[].class);
            int scale = unsafe.arrayIndexScale(Object[].class);
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

    private void orderedSet(int i, Object value) {
        unsafe.putOrderedObject(array, byteOffset(i), value);
    }
}
