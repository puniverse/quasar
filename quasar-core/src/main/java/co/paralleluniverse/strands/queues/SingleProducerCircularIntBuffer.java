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
public class SingleProducerCircularIntBuffer extends SingleProducerCircularWordBuffer<Integer> {
    public SingleProducerCircularIntBuffer(int size) {
        super(size);
    }

    @Override
    public void enq(Integer elem) {
        enq(elem.intValue());
    }

    public void enq(int elem) {
        enqRaw(elem);
    }

    @Override
    public IntConsumer newConsumer() {
        return new IntConsumer();
    }

    public class IntConsumer extends WordConsumer<Integer> {
        public int getIntValue() {
            return getRawValue();
        }

        @Override
        protected Integer getValue() {
            return getIntValue();
        }
    }
}
