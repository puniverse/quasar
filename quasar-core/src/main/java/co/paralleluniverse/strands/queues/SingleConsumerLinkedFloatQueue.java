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
public class SingleConsumerLinkedFloatQueue extends SingleConsumerLinkedWordQueue<Float> implements SingleConsumerFloatQueue<SingleConsumerLinkedQueue.Node<Float>> {
    @Override
    public boolean enq(float item) {
        return enqRaw(Float.floatToRawIntBits(item));
    }

    @Override
    public boolean enq(Float item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        return enq(item.floatValue());
    }

    @Override
    public float floatValue(Node<Float> node) {
        return Float.intBitsToFloat(rawValue(node));
    }

    @Override
    public Float value(Node<Float> node) {
        return floatValue(node);
    }
}
