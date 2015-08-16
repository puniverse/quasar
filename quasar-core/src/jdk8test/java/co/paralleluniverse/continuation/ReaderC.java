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
package co.paralleluniverse.continuation;

import co.paralleluniverse.common.reflection.ReflectionUtil;
import co.paralleluniverse.fibers.Callable;
import co.paralleluniverse.fibers.Suspend;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.ValuedContinuation;

public class ReaderC {
    @Suspendable // nesting
    public static <T, S extends ReaderScope, V> T runReader(Class<S> scope, V value, Callable<T> body) {
        ValuedContinuation<S, T, Void, V> c = new ValuedContinuation<>(scope, body);
        while (!c.isDone())
            c.run(value);
        return c.getResult();
    }

    // It would have been awesome if Java allowed generic exception classes so we could have <S extends ReaderScope<V>, V>
    public static <S extends ReaderScope, V> V get(Class<S> s) throws S {
        return ValuedContinuation.pause(ReflectionUtil.instance(s));
    }

    public static class ReaderScope extends Suspend {
    }
}
