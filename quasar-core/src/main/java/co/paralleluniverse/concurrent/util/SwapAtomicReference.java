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

import com.google.common.base.Function;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Extension of {@link AtomicReference} adding a swap operation à la Clojure atoms.
 *
 * @author circlespainter
 */
public class SwapAtomicReference<V> extends AtomicReference<V> {
    public V swap(final Function<V, V> f) {
        V newVal, currVal;
        do {
            currVal = get();
            newVal = f.apply(currVal);
        } while (!compareAndSet(currVal, newVal));
        return newVal;
    }
}
