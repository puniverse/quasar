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

public class StateC {
    @Suspendable // nesting
    public static <T, S extends StateScope, V> T runState(Class<S> scope, V initialState, Callable<T> body) {
        ValuedContinuation<S, T, V, V> c = new ValuedContinuation<>(scope, body);

        V state = initialState;
        c.run();
        while (!c.isDone()) {
            V v = c.getPauseValue();
            if (v != null) {
                state = v; // can be done purely with tail recursion
                c.run();
            } else {
                c.run(state);
            }
        }
        return c.getResult();
    }

    // It would have been awesome if Java allowed generic exception classes so we could have <S extends StateScope<V>, V>
    public static <S extends StateScope, V> void set(Class<S> s, V value) throws S {
        ValuedContinuation.pause(ReflectionUtil.instance(s), value);
    }

    // It would have been awesome if Java allowed generic exception classes so we could have <S extends StateScope<V>, V>
    public static <S extends StateScope, V> V get(Class<S> s) throws S {
        return ValuedContinuation.pause(ReflectionUtil.instance(s), (V) null);
    }

    public static class StateScope extends Suspend {
    }
}
