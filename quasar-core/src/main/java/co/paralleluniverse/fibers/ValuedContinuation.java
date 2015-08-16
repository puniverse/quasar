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
package co.paralleluniverse.fibers;

/**
 *
 * @author pron
 */
public class ValuedContinuation<S extends Suspend, T, Out, In> extends Continuation<S, T> {
    private Out pauseOut;
    private In pauseIn;
    private RuntimeException pauseInException;

    public ValuedContinuation(Class<S> scope, boolean detached, int stackSize, Callable<T> target) {
        super(scope, detached, stackSize, target);
    }

    public ValuedContinuation(Class<S> scope, boolean detached, Callable<T> target) {
        super(scope, detached, target);
    }

    public ValuedContinuation(Class<S> scope, Callable<T> target) {
        super(scope, target);
    }

    @Override
    protected ValuedContinuation<S, T, Out, In> self() {
        return (ValuedContinuation<S, T, Out, In>) super.self();
    }

    @Suspendable
    public void run(In value) {
        self().pauseIn = value;
        run();
    }

    @Suspendable
    public void run(RuntimeException ex) {
        self().pauseInException = ex;
        run();
    }

    public Out getPauseValue() {
        final ValuedContinuation<S, T, Out, In> self = self();
        // System.err.println("getPauseValue: " + self.pauseOut + " " + this);
        Out v = self.pauseOut;
        self.pauseOut = null;
        return v;
    }

    public static <S extends Suspend, In> In pause(S scope) throws S {
        return inValue((ValuedContinuation<S, ?, ?, In>) Continuation.suspend(scope));
    }

    public static <S extends Suspend, Out, In> In pause(S scope, final Out value) throws S {
        return inValue((ValuedContinuation<S, ?, Out, In>) Continuation.suspend(scope, new CalledCC<S>() {
            @Override
            public <T> void suspended(Continuation<S, T> c) {
                // System.err.println("setPauseValue: " + value + " " + c);
                ((ValuedContinuation<S, ?, Out, In>) c).pauseOut = value;
            }
        }));
    }

    public static <S extends Suspend, Out, In> In pause(S scope, final CalledCCReturn<S, Out> ccc) throws S {
        return inValue((ValuedContinuation<S, ?, Out, In>) Continuation.suspend(scope, new CalledCC<S>() {
            @Override
            public <T> void suspended(Continuation<S, T> c) {
                ValuedContinuation<S, ?, Out, In> vc = (ValuedContinuation<S, ?, Out, In>) c;
                vc.pauseOut = ccc.suspended(vc);
            }
        }));
    }

    public static <S extends Suspend, Out, In> In pause(S scope, final CalledCCV<S> ccc) throws S {
        return inValue((ValuedContinuation<S, ?, Out, In>) Continuation.suspend(scope, ccc));
    }

    public static <S extends Suspend, Out, X, In> In pause(S scope, final X x, final CalledCCArg<S, X> ccc) throws S {
        return inValue((ValuedContinuation<S, ?, Out, In>) Continuation.suspend(scope, new CalledCC<S>() {

            @Override
            public <T> void suspended(Continuation<S, T> c) {
                ccc.suspended((ValuedContinuation<S, ?, Out, In>) c, x);
            }
        }));
    }

    public static <S extends Suspend, Out, X, In> In pause(S scope, final X x, final CalledCCArgReturn<S, X, Out> ccc) throws S {
        return inValue((ValuedContinuation<S, ?, Out, In>) Continuation.suspend(scope, new CalledCC<S>() {

            @Override
            public <T> void suspended(Continuation<S, T> c) {
                ValuedContinuation<S, ?, Out, In> vc = (ValuedContinuation<S, ?, Out, In>) c;
                vc.pauseOut = ccc.suspended(vc, x);
            }
        }));
    }

    private static <S extends Suspend, T, Out, In> In inValue(ValuedContinuation<S, T, Out, In> c) {
        In res = c.pauseIn;
        RuntimeException ex = c.pauseInException;
        c.pauseIn = null;
        c.pauseInException = null;
        if (ex != null)
            throw ex;
        else
            return res;
    }
}
