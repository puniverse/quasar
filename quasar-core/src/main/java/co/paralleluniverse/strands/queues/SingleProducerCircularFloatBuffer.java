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
public class SingleProducerCircularFloatBuffer extends SingleProducerCircularWordBuffer<Float> {
    public SingleProducerCircularFloatBuffer(int size) {
        super(size);
    }

    @Override
    public void enq(Float elem) {
        enq(elem.floatValue());
    }

    public void enq(float elem) {
        enqRaw(Float.floatToRawIntBits(elem));
    }

    @Override
    public FloatConsumer newConsumer() {
        return new FloatConsumer();
    }

    public class FloatConsumer extends WordConsumer<Float> {
        public float getFloatValue() {
            return Float.intBitsToFloat(getRawValue());
        }

        @Override
        protected Float getValue() {
            return getFloatValue();
        }
    }
}
