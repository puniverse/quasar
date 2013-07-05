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
public class SingleConsumerLinkedArrayFloatQueue extends SingleConsumerLinkedArrayWordQueue<Float>
        implements SingleConsumerFloatQueue<SingleConsumerLinkedArrayQueue.ElementPointer>, BasicSingleConsumerFloatQueue {
    @Override
    public boolean enq(float element) {
        return enqRaw(Float.floatToRawIntBits(element));
    }

    @Override
    public boolean enq(Float element) {
        return enq(element.floatValue());
    }

    @Override
    public Float value(ElementPointer node) {
        return floatValue(node);
    }

    @Override
    public float floatValue(ElementPointer node) {
        return Float.intBitsToFloat(rawValue(node.n, node.i));
    }

    @Override
    public float pollFloat() {
        final ElementPointer n = pk();
        final float val = floatValue(n);
        deq(n);
        return val;
    }
}
