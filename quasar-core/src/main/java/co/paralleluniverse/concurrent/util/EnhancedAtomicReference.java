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
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Extension of {@link AtomicReference} with useful methods based on CAS.
 *
 * @author circlespainter
 */
public class EnhancedAtomicReference<V> extends AtomicReference<V> {
    public void swap(final Function<V, V> f) {
        V newVal, currVal;
        do {
            currVal = get();
            newVal = f.apply(currVal);
        } while (!compareAndSet(currVal, newVal));
    }
}
