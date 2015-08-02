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
import static co.paralleluniverse.fibers.Continuation.*;
import co.paralleluniverse.fibers.SuspendExecution;
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

    @Suspendable // nested
    public T run() throws NoSolution {
        AmbContinuation<T> c;
        while (true) {
            try {
                c = pop();
                c.run();
                // assert c.isDone() : "Not done: " + c;
                if (c.isDone())
                    return c.getResult();
            } catch (RuntimeNoSolution e) {
                throw new NoSolution();
            }
        }
    }

    public boolean hasRemaining() {
        System.err.println("HAS_REMAINING: " + cs);
        return !cs.isEmpty();
    }

    private void push(Continuation<?, ?> c) {
        System.err.println("PUSH: " + c);
        cs.addFirst((AmbContinuation<T>) c);
    }

    private AmbContinuation<T> pop() {
        if (cs.isEmpty())
            throw new RuntimeNoSolution();
        AmbContinuation<T> c = cs.removeFirst();
        System.err.println("POP: " + c);
        return c;
    }

    private static class AmbContinuation<T> extends Continuation<AmbScope, T> {
        private final Ambiguity<T> ambiguity;
        private boolean isClone = true; // trick

        public AmbContinuation(Ambiguity<T> ambiguity, final Ambiguous<T> target) {
            super(AmbScope.class, new Callable<T>() {
                @Override
                @Suspendable
                public T call() {
                    try {
                        return target.run();
                    } catch (SuspendExecution e) {
                        throw new AssertionError(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            this.ambiguity = ambiguity;
        }
    }

    public static <T> T amb(final List<T> values) throws AmbScope {
        if (values.size() > 1) {
            AmbContinuation<T> c;
            do {
                c = (AmbContinuation<T>) suspend(SCOPE, CAPTURE);
            } while (c.isClone && values.size() > 1); // will run once for each clone
        }
        System.err.println("XXXX AMB: " + values);
        return values.remove(0);
    }

    public static <T> T amb(T... values) throws AmbScope {
        return amb(new ArrayList<>(Arrays.asList(values)));
    }

    public static void assertThat(boolean pred) throws AmbScope {
        if (!pred) {
            System.err.println("ASSERT FAILED");
            suspend(SCOPE, BACKTRACK);
        }
    }

//    private static AmbContinuation<?> captureCurrentContinuation() throws AmbScope {
//        return (AmbContinuation<?>) suspend(SCOPE, new CalledCC<AmbScope>() {
//            @Override
//            public <T> Continuation<AmbScope, T> suspended(Continuation<AmbScope, T> c) {
//                return c;
//            }
//        }).clone();
//    }
    public static class AmbScope extends Suspend {
    }

    private static final AmbScope SCOPE = new AmbScope();

//    private static final CalledCC<AmbScope> CONTINUE = new CalledCC<AmbScope>() {
//        @Override
//        public <T> Continuation<AmbScope, T> suspended(Continuation<AmbScope, T> c) {
//            return c;
//        }
//    };
    private static final CalledCC<AmbScope> CAPTURE = new CalledCC<AmbScope>() {
        @Override
        public <T> void suspended(Continuation<AmbScope, T> c) {
            System.err.println("QQQQQQQQQQ");
            try {
                AmbContinuation<T> a = (AmbContinuation<T>) c;
                a.ambiguity.push(a.clone());
                a.isClone = false; // trick: done _after_ the clone
                a.run();           // continue running
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    };

    private static final CalledCC<AmbScope> BACKTRACK = null;

    public static class NoSolution extends Exception {
    }

    private static class RuntimeNoSolution extends RuntimeException {
    }

    public static interface Ambiguous<T> {
        T run() throws AmbScope, SuspendExecution, InterruptedException; // Unfortunately, throwables can't be generic. We would have wanted to CoIteratorScope<E>
    }
}
