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
public abstract class CircularBuffer<E> implements BasicQueue<E> {
    final int capacity;
    final int mask;
    private final boolean singleProducer;
    volatile long p101, p102, p103, p104, p105, p106, p107;
    volatile long tail; // next element to be written
    volatile long p201, p202, p203, p204, p205, p206, p207;
    volatile long lastWritten; // follows tail
    volatile Object p301, p302, p303, p304, p305, p306, p307;
    final Consumer consumer;

    CircularBuffer(int capacity, boolean singleProducer) {
        // capacity is a power of 2
        this.capacity = nextPowerOfTwo(capacity);
        this.mask = this.capacity - 1;
        this.singleProducer = singleProducer;
        this.consumer = newConsumer();
    }

    public boolean isSingleProducer() {
        return singleProducer;
    }

    private static int nextPowerOfTwo(int v) {
        assert v >= 0;
        return 1 << (32 - Integer.numberOfLeadingZeros(v - 1));
    }

    @Override
    public int capacity() {
        return capacity;
    }

    final long preEnq() {
        long t;

        if (singleProducer) {
            t = tail;
            tail++; // orderedSetTail(t + 1); // 
        } else {
            do {
                t = tail;
            } while (!casTail(t, t + 1));
        }
        return t;
    }

    final void postEnq() {
        if (singleProducer)
            lastWritten++;
        else {
            long w;
            do {
                w = lastWritten;
            } while (!casLastWritten(w, w + 1));
        }
    }

    @Override
    public abstract boolean enq(E elem);

    @Override
    public E poll() {
        return consumer.poll();
    }

    @Override
    public int size() {
        return consumer.size();
    }

    public abstract Consumer newConsumer();

    public abstract class Consumer {
        protected long head;

        public final long lastIndexRead() {
            return head - 1;
        }

        public final boolean hasNext() {
            return tail > head;
        }

        public void poll0() {
            assert tail > head;
            int headStart = 0; // how many elements ahead of tail we'll try to read if tail progresses too fast
            int attempt = 0;
            for (;;) {
                while (lastWritten < head) // wait for enq to complete 
                        ;
                grabValue((int) head & mask);
                final long oldest = tail - capacity;
                if (head >= oldest) {
                    head++;
                    return;
                }
                // tail has overtaken us
                head = oldest + headStart; // < tail
                if (attempt > 30)
                    throw new RuntimeException("Can't catch up with producer");
                // increasing headStart diesn't work for some reason. it breaks monotonicity somehow...
//                if ((++attempt & 0x03) == 0) { // every 4 attempts inc headStart
//                    headStart++;
//                    if (headStart >= capacity)
//                        throw new RuntimeException("Can't catch up with producer");
//                }
                attempt++;
            }
        }

        public E poll() {
            poll0();
            final E v = getValue();
            clearValue(); // for gc
            return v;
        }

        public E getAndClearReadValue() {
            final E v = getValue();
            clearValue(); // for gc
            return v;
        }

        public int size() {
            return (int) (tail - head);
        }

        protected abstract void grabValue(int index);

        protected abstract void clearValue();

        protected abstract E getValue();
    }
    ////////////////////////////////////////////////////////////////////////
    static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long tailOffset;
    private static final long lastWrittenOffset;

    static {
        try {
            tailOffset = unsafe.objectFieldOffset(CircularBuffer.class.getDeclaredField("tail"));
            lastWrittenOffset = unsafe.objectFieldOffset(CircularBuffer.class.getDeclaredField("lastWritten"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private void orderedSetTail(long value) {
        unsafe.putOrderedLong(this, tailOffset, value);
    }

    boolean casTail(long expected, long update) {
        return unsafe.compareAndSwapLong(this, tailOffset, expected, update);
    }

    boolean casLastWritten(long expected, long update) {
        return unsafe.compareAndSwapLong(this, lastWrittenOffset, expected, update);
    }
}
