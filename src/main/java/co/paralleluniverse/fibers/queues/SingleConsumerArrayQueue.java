/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.queues;

import co.paralleluniverse.concurrent.util.UtilUnsafe;
import java.util.Iterator;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
abstract class SingleConsumerArrayQueue<E> extends SingleConsumerQueue<E, Integer> {
    volatile int head; // next element to be read
    volatile Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    volatile int tail; // next element to be written


    @Override
    public boolean allowRetainPointers() {
        return false;
    }

    @Override
    public E value(Integer index) {
        return value(index.intValue());
    }

    public abstract E value(int index);
    
    abstract int arrayLength();
    
    abstract void awaitValue(int index);
    
    abstract void clearValue(int index);
    
    abstract void copyValue(int to, int from);
    
    int maxReadIndex() {
        return tail;
    }
    
    final int preEnq() {
        if (next(tail) == head)
            throw new QueueCapacityExceededException();

        int t;
        for (;;) {
            t = tail;
            if (compareAndSetTail(t, next(t)))
                break;
        }
        return t;
    }

    @Override
    public void deq(Integer index) {
        deq(index.intValue());
    }

    public void deq(int index) {
        final int newHead = next(index);
        for (int i = head; i != newHead; i = next(i))
            clearValue(i);
        head = newHead;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public Integer pk() {
        if (head == maxReadIndex())
            return null;
        awaitValue(head);
        return Integer.valueOf(head);
    }

    @Override
    public Integer succ(Integer index) {
        final int s = succ(index != null ? index.intValue() : -1);
        return s >= 0 ? Integer.valueOf(s) : null;
    }

    @SuppressWarnings("empty-statement")
    public int succ(int index) {
        if (index < 0) {
            final Integer pk = pk();
            return pk != null ? pk : -1;
        }
        int n = index;
        n = next(n);
        if (n == maxReadIndex())
            return -1;
        awaitValue(n);
        return n;
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

        clearValue(index);
        int t = tail;
        if (index == t) {
            if (compareAndSetTail(t, prev(t)))
                return prev(index);
        }

        final int h = head;
        int i = index;
        while (i != h) {
            int pi = prev(i);
            copyValue(i, pi);
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
            return tail + (arrayLength() - head);
    }

    int next(int i) {
        return (i + 1) % arrayLength();
        //return (++i == array.length) ? 0 : i;
    }

    int prev(int i) {
        return (--i == -1) ? (arrayLength() - 1) : i;
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
    static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long tailOffset;

    static {
        try {
            tailOffset = unsafe.objectFieldOffset(SingleConsumerArrayQueue.class.getDeclaredField("tail"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    /**
     * CAS tail field. Used only by preEnq.
     */
    private boolean compareAndSetTail(int expect, int update) {
        return unsafe.compareAndSwapInt(this, tailOffset, expect, update);
    }
}
