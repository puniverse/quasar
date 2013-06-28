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
public abstract class SingleProducerCircularBuffer<E> {
    final int capacity;
    final int mask;
    volatile long p101, p102, p103, p104, p105, p106, p107;
    volatile long tail; // next element to be written
    volatile long p201, p202, p203, p204, p205, p206, p207;
    volatile long lastWritten; // follows tail

    SingleProducerCircularBuffer(int capacity) {
        // capacity is a power of 2
        this.capacity = capacity;
        this.mask = capacity - 1;
    }

    static int nextPowerOfTwo(int v) {
        assert v >= 0;
        return 1 << (32 - Integer.numberOfLeadingZeros(v - 1));
    }

    public int capacity() {
        return capacity;
    }

    final long preEnq() {
        final long t = tail;
        tail++; // orderedSetTail(t + 1); // 
        return t;
    }

    final void postEnq() {
        lastWritten++;
    }

    public abstract void enq(E elem);

    public abstract Consumer<E> newConsumer();

    public abstract class Consumer<E> {
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
                grabValue((int) head & mask);
                final long oldest = tail - capacity;
                if (head >= oldest) {
                    head++;
                    
                    while (lastWritten < head) // wait for enq to complete 
                        ;
                    return;
                }
                // tail has overtaken us
                head = oldest + headStart; // < tail
                if(attempt > 30)
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

        protected abstract void grabValue(int index);

        protected abstract void clearValue();

        protected abstract E getValue();
    }
    ////////////////////////////////////////////////////////////////////////
    static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long tailOffset;

    static {
        try {
            tailOffset = unsafe.objectFieldOffset(SingleProducerCircularBuffer.class.getDeclaredField("tail"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private void orderedSetTail(long value) {
        unsafe.putOrderedLong(this, tailOffset, value);
    }
}
