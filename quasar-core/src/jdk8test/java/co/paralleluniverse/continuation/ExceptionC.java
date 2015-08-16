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
import com.google.common.base.Function;

public class ExceptionC {
    @Suspendable // nesting
    public static <T, S extends ExceptionScope> T tryc(Callable<T> body, Class<S> scope, Function<S, T> catchc) {
        ValuedContinuation<S, T, S, Void> c = new ValuedContinuation<>(scope, body);
        c.run();
        return c.isDone() ? c.getResult() : catchc.apply(c.getPauseValue());
    }
    
    public static <S extends ExceptionScope> void throwc(S ex) throws S {
        ValuedContinuation.pause(ex, ex);
    }

    public static class ExceptionScope extends Suspend {
        public final String message;

        public ExceptionScope(String message) {
            this.message = message;
        }
    }
}
