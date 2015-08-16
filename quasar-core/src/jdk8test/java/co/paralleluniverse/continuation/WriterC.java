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

import co.paralleluniverse.fibers.Callable;
import co.paralleluniverse.fibers.Suspend;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.ValuedContinuation;
import java.util.ArrayList;
import java.util.List;

public class WriterC<S extends WriterC.WriterScope, V> {
    private final List<V> list = new ArrayList<>();
    private final Class<S> scope;

    public WriterC(Class<S> scope) {
        this.scope = scope;
    }

    @Suspendable // nesting
    public <T> T runWriter(Callable<T> body) {
        ValuedContinuation<S, T, V, Void> c = new ValuedContinuation<>(scope, body);

        while (true) {
            c.run();
            if (c.isDone())
                break;
            list.add(c.getPauseValue()); // can be pure
        }
        return c.getResult();
    }

    public List<V> getList() {
        return list;
    }

    // It would have been awesome if Java allowed generic exception classes so we could have <S extends WriterScope<V>, V>
    public static <S extends WriterScope, V> void add(Class<S> s, V value) throws S {
        try {
            ValuedContinuation.pause(s.newInstance(), value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    public static class WriterScope extends Suspend {
    }
}
