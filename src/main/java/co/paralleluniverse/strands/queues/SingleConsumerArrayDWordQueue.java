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
abstract class SingleConsumerArrayDWordQueue<E> extends SingleConsumerArrayPrimitiveQueue<E> {
    private final long[] array;

    public SingleConsumerArrayDWordQueue(int capacity) {
        super(nextPowerOfTwo(capacity));
        this.array = new long[nextPowerOfTwo(capacity)];
    }

    long rawValue(int index) {
        return array[index];
    }

    @Override
    int arrayLength() {
        return array.length;
    }

    boolean enq(long item) {
        final long i = preEnq();
        if(i < 0)
            return false;
        array[(int) i & mask] = item; // no need for volatile semantics because postEnq does a volatile write (cas) which is then read in await value
        postEnq(i);
        return true;
    }

    @Override
    void copyValue(int to, int from) {
        array[to] = array[from];
    }
}
