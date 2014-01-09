/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
public class CircularLongBuffer extends CircularDWordBuffer<Long> implements BasicSingleConsumerLongQueue {
    public CircularLongBuffer(int size, boolean singleProducer) {
        super(size, singleProducer);
    }

    @Override
    public boolean enq(Long elem) {
        return enq(elem.longValue());
    }

    @Override
    public boolean enq(long elem) {
        enqRaw(elem);
        return true;
    }

    @Override
    public long pollLong() {
        return ((LongConsumer) consumer).pollLong();
    }

    @Override
    public LongConsumer newConsumer() {
        return new LongConsumer();
    }

    public class LongConsumer extends DWordConsumer {
        public long getLongValue() {
            return getRawValue();
        }

        @Override
        protected Long getValue() {
            return getLongValue();
        }

        public long pollLong() {
            poll0();
            return getLongValue();
        }
    }
}
