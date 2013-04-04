/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

import co.paralleluniverse.concurrent.util.UtilUnsafe;
import java.util.Iterator;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayQueue<E> extends SingleConsumerQueue<E, Integer> {
    private final Object[] array;
    private volatile int head; // next element to be read
    Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    private volatile int tail; // next element to be written

    public SingleConsumerArrayQueue(int size) {
        this.array = new Object[size];
    }

    @Override
    public boolean allowRetainPointers() {
        return false;
    }
    
    @Override
    public E value(Integer index) {
        return value(index.intValue());
    }

    public E value(int index) {
        return (E)array[index];
    }

    @Override
    public void enq(E item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        if (next(tail) == head)
            throw new RuntimeException("Queue capacity exceeeded");

        int t;
        for (;;) {
            t = tail;
            if (compareAndSetTail(t, next(t)))
                break;
        }
        set(t, item);
    }

    @Override
    public void deq(Integer index) {
        deq(index.intValue());
    }

    public void deq(int index) {
        final int newHead = next(index);
        for (int i = head; i != newHead; i = next(i))
            lazySet(i, null);
        head = newHead;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public Integer pk() {
        if (head == tail)
            return null;
        while (get(head) == null); // volatile read
        return Integer.valueOf(head);
    }

    @Override
    public Integer succ(Integer index) {
        final int s = succ(index != null ? index.intValue() : -1);
        return s >= 0 ? Integer.valueOf(s) : null;
    }

    @SuppressWarnings("empty-statement")
    public int succ(int index) {
        if(index < 0) {
            final Integer pk = pk();
            return pk != null ? pk : -1;
        }
        int n = index;
        for (;;) {
            n = next(n);
            if (n == tail)
                return -1;
            while (get(n) == null); // volatile read
        }
    }

    @Override
    public Integer del(Integer index) {
        return del(index.intValue());
    }

    public int del(int index) {
        if (index == head) {
            deq(index);
            return -1;
        }

        lazySet(index, null);
        int t = tail;
        if (index == t) {
            if (compareAndSetTail(t, prev(t)))
                return prev(index);
        }

        final int h = head;
        int i = index;
        while (i != h) {
            int pi = prev(i);
            lazySet(i, array[pi]);
            i = pi;
        }
        head = next(h);
        return index;
    }

    @Override
    public int size() {
        if (tail >= head)
            return tail - head;
        else
            return tail + (array.length - head);
    }

    private int next(int i) {
        return (i + 1) % array.length;
        //return (++i == array.length) ? 0 : i;
    }

    private int prev(int i) {
        return (--i == -1) ? (array.length - 1) : i;
    }

    @Override
    public Iterator<E> iterator() {
        return new QueueIterator();
    }

    @Override
    public void resetIterator(Iterator<E> iter) {
        ((QueueIterator) iter).n = -1;
    }

    private class QueueIterator implements Iterator<E> {
        private int n = -1;

        @Override
        public boolean hasNext() {
            return succ(n) >= 0;
        }

        @Override
        public E next() {
            n = succ(n);
            return value(n);
        }

        @Override
        public void remove() {
            n = del(n);
        }
    }
    ////////////////////////////////////////////////////////////////////////
    private static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long tailOffset;
    private static final int base;
    private static final int shift;

    static {
        try {
            tailOffset = unsafe.objectFieldOffset(SingleConsumerArrayQueue.class.getDeclaredField("tail"));
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

    /**
     * CAS tail field. Used only by enq.
     */
    private boolean compareAndSetTail(int expect, int update) {
        return unsafe.compareAndSwapInt(this, tailOffset, expect, update);
    }

    private void set(int i, Object value) {
        unsafe.putObjectVolatile(array, byteOffset(i), value);
    }

    private void lazySet(int i, Object value) {
        unsafe.putOrderedObject(array, byteOffset(i), value);
    }

    private Object get(int i) {
        return unsafe.getObjectVolatile(array, byteOffset(i));
    }
}
