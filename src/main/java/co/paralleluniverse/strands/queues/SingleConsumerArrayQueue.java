/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

import co.paralleluniverse.concurrent.util.UtilUnsafe;
import java.util.Iterator;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
abstract class SingleConsumerArrayQueue<E> extends SingleConsumerQueue<E, Integer> {
    private final int mask;
    volatile int p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    volatile int head; // next element to be read
    volatile int p101, p102, p103, p104, p105, p106, p107, p108, p109, p110, p111, p112, p113, p114, p115;
    volatile int tail; // next element to be written
    volatile int p201, p202, p203, p204, p205, p206, p207, p208, p209, p210, p211, p212, p213, p214, p215;
    private int cachedHead;
    volatile int p301, p302, p303, p304, p305, p306, p307, p308, p309, p310, p311, p312, p313, p314, p315;
    private int cachedMaxReadIndex;

    SingleConsumerArrayQueue(int size) {
        // size is a power of 2
        this.mask = size - 1;
    }

    // taken from http://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
    static int nextPowerOfTwo(int v) {
        assert v >= 0;
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v++;
        return v;
    }

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

    @Override
    public boolean isFull() {
        final int nextTail = next(tail);
        if (nextTail == cachedHead) {
            cachedHead = head; // only time a producer reads head

            if (nextTail == cachedHead)
                return true;
        }
        return false;
    }

    
    final int preEnq() {
        if(isFull())
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
        orderedSetHead(newHead);//head = newHead;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public Integer pk() {
        final int h = head;
        if (h == cachedMaxReadIndex) {
            cachedMaxReadIndex = maxReadIndex();
            if (h == cachedMaxReadIndex)
                return null;
        }
        awaitValue(h);
        return Integer.valueOf(h);
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
        if (n == cachedMaxReadIndex) {
            cachedMaxReadIndex = maxReadIndex();
            if (n == cachedMaxReadIndex)
                return -1;
        }
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
        orderedSetHead(next(h));//head = next(h);
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
        return (i + 1) & mask;
        //return (++i == array.length) ? 0 : i;
    }

    int prev(int i) {
        return --i & mask;
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
    private static final long headOffset;
    private static final long tailOffset;

    static {
        try {
            headOffset = unsafe.objectFieldOffset(SingleConsumerArrayQueue.class.getDeclaredField("head"));
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

    private void orderedSetHead(int value) {
        unsafe.putOrderedInt(this, headOffset, value);
    }
}
