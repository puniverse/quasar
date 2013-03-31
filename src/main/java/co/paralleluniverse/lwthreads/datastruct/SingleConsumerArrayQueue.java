/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

import co.paralleluniverse.concurrent.util.UtilUnsafe;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayQueue<E> implements SingleConsumerQueue<E, Integer> {
    private final E[] array;
    private volatile int head; // next element to be read
    Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    private volatile int tail; // next element to be written

    public SingleConsumerArrayQueue(int size) {
        this.array = (E[]) new Object[size];
    }

    @Override
    public E value(Integer index) {
        return value(index.intValue());
    }

    public E value(int index) {
        return array[index];
    }

    @Override
    public void enq(E element) {
        if (next(tail) == head)
            throw new RuntimeException("Queue capacity exceeeded");

        int t, nt;
        for (;;) {
            t = tail;
            nt = next(t);
            if (compareAndSetTail(t, nt))
                break;
        }
        lazySet(t, element);
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
    public Integer peek() {
        if(head == tail)
            return null;
        get(head); // volatile read
        return Integer.valueOf(head);
    }

    @Override
    public Integer succ(Integer index) {
        return Integer.valueOf(succ(index.intValue()));
    }

    public int succ(int index) {
        int n = next(index);
        get(n); // volatile read
        return n;
    }

    @Override
    public void del(Integer index) {
        del(index.intValue());
    }
    
    public void del(int index) {
        if(index == head) {
            deq(index);
            return;
        }
        
        lazySet(index, null);
        int t = tail;
        if(index == t) {
            if (compareAndSetTail(t, prev(t)))
                return;
        }
        
        final int h = head;
        int i = index;
        while(i != h) {
            int pi = prev(i);
            lazySet(i, array[pi]);
            i = pi;
        }
        head = next(h);
    }

    @Override
    public int size() {
        if(tail > head)
            return tail - head;
        else
            return head + (array.length - tail);
    }

    private int next(int i) {
        return (i + 1) % array.length;
        //return (++i == array.length) ? 0 : i;
    }
    private int prev(int i) {
        return (--i == -1) ? (array.length - 1) : i;
    }
    ////////////////////////////////////////////////////////////////////////
    protected static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long tailOffset;
    private static final long arrayOffset;
    private static final int base;
    private static final int shift;

    static {
        try {
            tailOffset = unsafe.objectFieldOffset(SingleConsumerArrayQueue.class.getDeclaredField("tail"));
            arrayOffset = unsafe.objectFieldOffset(SingleConsumerArrayQueue.class.getDeclaredField("array"));
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
     * CAS tail field. Used only by enq.
     */
    boolean compareAndSetTail(int expect, int update) {
        return unsafe.compareAndSwapInt(this, tailOffset, expect, update);
    }

    private void set(int i, E value) {
        unsafe.putObjectVolatile(array, byteOffset(i), value);
    }

    private void lazySet(int i, E value) {
        unsafe.putOrderedObject(array, byteOffset(i), value);
    }

    private E get(int i) {
        return (E) unsafe.getObjectVolatile(array, byteOffset(i));
    }

    private long checkedByteOffset(int i) {
        if (i < 0 || i >= array.length)
            throw new IndexOutOfBoundsException("index " + i);

        return byteOffset(i);
    }

    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }
}
