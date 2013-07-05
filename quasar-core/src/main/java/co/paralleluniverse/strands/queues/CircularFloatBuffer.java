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
public class CircularFloatBuffer extends CircularWordBuffer<Float> implements BasicSingleConsumerFloatQueue {
    public CircularFloatBuffer(int size, boolean singleProducer) {
        super(size, singleProducer);
    }

    @Override
    public boolean enq(Float elem) {
        return enq(elem.floatValue());
    }

    @Override
    public boolean enq(float elem) {
        enqRaw(Float.floatToRawIntBits(elem));
        return true;
    }

    @Override
    public float pollFloat() {
        return ((FloatConsumer) consumer).pollFloat();
    }

    @Override
    public FloatConsumer newConsumer() {
        return new FloatConsumer();
    }

    public class FloatConsumer extends WordConsumer {
        public float getFloatValue() {
            return Float.intBitsToFloat(getRawValue());
        }

        @Override
        protected Float getValue() {
            return getFloatValue();
        }

        public float pollFloat() {
            poll0();
            return getFloatValue();
        }
    }
}
