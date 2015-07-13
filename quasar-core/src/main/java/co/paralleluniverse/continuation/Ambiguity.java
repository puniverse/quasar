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
import co.paralleluniverse.fibers.CalledCC;
import co.paralleluniverse.fibers.Continuation;
import co.paralleluniverse.fibers.Suspend;
import static co.paralleluniverse.fibers.Continuation.suspend;
import co.paralleluniverse.fibers.Suspendable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 *
 * @author pron
 */
public class Ambiguity<T> {
    public static <T> Ambiguity<T> solve(Ambiguous<T> f) {
        return new Ambiguity<T>(f);
    }

    private final Deque<AmbContinuation<T>> cs = new ArrayDeque<>();

    public Ambiguity(Ambiguous<T> ambiguity) {
        push(new AmbContinuation<T>(this, ambiguity));
    }

    public T run() throws NoSolution {
        try {
            Continuation<AmbScope, T> c = pop().go();
            assert c.isDone();
            return c.getResult();
        } catch (RuntimeNoSolution e) {
            throw new NoSolution();
        }
    }

    public boolean hasRemaining() {
        return !cs.isEmpty();
    }

    private void push(AmbContinuation<?> c) {
        cs.addFirst((AmbContinuation<T>) c);
    }

    private AmbContinuation<T> pop() {
        if (cs.isEmpty())
            throw new RuntimeNoSolution();
        return cs.removeFirst();
    }

    private static class AmbContinuation<T> extends Continuation<AmbScope, T> {
        private final Ambiguity<T> ambiguity;

        public AmbContinuation(Ambiguity<T> ambiguity, final Ambiguous<T> target) {
            super(AmbScope.class, new Callable<T>() {
                @Suspendable
                @Override
                public T call() {
                    return target.run();
                }
            });
            this.ambiguity = ambiguity;
        }
    }

    public static <T> T amb(List<T> values) throws AmbScope {
        AmbContinuation<?> c = captureCurrentContinuation();
        final T v = values.remove(0);
        if (!values.isEmpty())
            c.ambiguity.push(c);
        return v;
    }

    public static <T> T amb(T... values) throws AmbScope {
        return amb(new ArrayList<>(Arrays.asList(values)));
    }

    public static void assertThat(boolean pred) throws AmbScope {
        if (!pred)
            suspend(SCOPE, BACKTRACK);
    }

    private static AmbContinuation<?> captureCurrentContinuation() throws AmbScope {
        return (AmbContinuation<?>) suspend(SCOPE, CONTINUE).clone();
    }

    public static class AmbScope extends Suspend {
    }

    private static final AmbScope SCOPE = new AmbScope();

    private static final CalledCC<AmbScope> CONTINUE = new CalledCC<AmbScope>() {
        @Override
        public <T> Continuation<AmbScope, T> suspended(Continuation<AmbScope, T> c) {
            return c;
        }
    };

    private static final CalledCC<AmbScope> BACKTRACK = new CalledCC<AmbScope>() {
        @Override
        public <T> Continuation<AmbScope, T> suspended(Continuation<AmbScope, T> c) {
            return ((AmbContinuation<T>) c).ambiguity.pop();
        }
    };

    public static class NoSolution extends Exception {
    }

    private static class RuntimeNoSolution extends RuntimeException {
    }

    public static interface Ambiguous<T> {
        T run() throws AmbScope; // Unfortunately, throwables can't be generic. We would have wanted to CoIteratorScope<E>
    }
}
