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
public class SingleProducerCircularDoubleBuffer extends SingleProducerCircularDWordBuffer<Double> {
    public SingleProducerCircularDoubleBuffer(int size, boolean singleProducer) {
        super(size, singleProducer);
    }

    @Override
    public void enq(Double elem) {
        enq(elem.doubleValue());
    }

    public void enq(double elem) {
        enqRaw(Double.doubleToRawLongBits(elem));
    }

    @Override
    public DoubleConsumer newConsumer() {
        return new DoubleConsumer();
    }

    public class DoubleConsumer extends DWordConsumer<Double> {
        public double getDoubleValue() {
            return Double.longBitsToDouble(getRawValue());
        }

        @Override
        protected Double getValue() {
            return getDoubleValue();
        }
    }
}
