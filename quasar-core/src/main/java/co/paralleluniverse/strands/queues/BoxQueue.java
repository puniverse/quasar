/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 *
 * @author pron
 */
public class BoxQueue<E> implements BasicQueue<E> {
    private final boolean replaceOnWrite;
    private final boolean singleConsumer;
    private volatile E value;

    public BoxQueue(boolean replaceOnWrite, boolean singleConsumer) {
        this.replaceOnWrite = replaceOnWrite;
        this.singleConsumer = singleConsumer;
    }

    @Override
    public int capacity() {
        return 1;
    }

    @Override
    public int size() {
        return value != null ? 1 : 0;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }
    
    @Override
    public boolean enq(E element) {
        assert element != null;
        if (replaceOnWrite) {
            value = element;
            return true;
        } else
            return casValue(null, element);
    }

    @Override
    public E poll() {
        E v;
        if (singleConsumer) {
            v = value;
            if (v != null) {
                if (replaceOnWrite)
                    casValue(v, null); // we don't care about the result
                else
                    value = null;
            }
        } else {
            do {
                v = value;
            } while (v != null && !casValue(v, null));
        }
        return v;
    }
    //////////
    private static final VarHandle VALUE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(BoxQueue.class, "value", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    boolean casValue(Object expected, Object update) {
        return VALUE.compareAndSet(this, expected, update);
    }
}
