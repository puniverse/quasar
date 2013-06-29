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
public class SingleProducerCircularLongBuffer extends SingleProducerCircularDWordBuffer<Long> {
    public SingleProducerCircularLongBuffer(int size, boolean singleProducer) {
        super(size, singleProducer);
    }

    @Override
    public void enq(Long elem) {
        enq(elem.longValue());
    }

    public void enq(long elem) {
        enqRaw(elem);
    }

    @Override
    public LongConsumer newConsumer() {
        return new LongConsumer();
    }

    public class LongConsumer extends DWordConsumer<Long> {
        public long getLongValue() {
            return getRawValue();
        }

        @Override
        protected Long getValue() {
            return getLongValue();
        }
    }
}
