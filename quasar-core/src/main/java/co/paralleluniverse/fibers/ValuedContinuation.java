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

import com.google.common.base.Function;

/**
 *
 * @author pron
 */
public class ValuedContinuation<S extends Suspend, T, Out, In> extends Continuation<S, T> {
    private Out pauseOut;
    private In pauseIn;

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
    protected void prepare() {
        super.prepare();
        pauseOut = null;
    }

    public void run(In value) {
        pauseIn = value;
        run();
    }

    public static <S extends Suspend, In> In pause(S scope) throws S {
        ValuedContinuation<S, ?, ?, In> c = (ValuedContinuation<S, ?, ?, In>) suspend(scope);
        In res = c.pauseIn;
        c.pauseIn = null;
        return res;
    }

    public static <S extends Suspend, T, Out, In> In pause(S scope, final Out value) throws S {
        ValuedContinuation<S, ?, Out, In> c = (ValuedContinuation<S, ?, Out, In>) Continuation.suspend(scope, new CalledCC<S, T>() {
            @Override
            public Continuation<S, T> suspended(Continuation<S, T> c) {
                ((ValuedContinuation<S, ?, Out, In>) c).pauseOut = value;
                return null;
            }
        });
        In res = c.pauseIn;
        c.pauseIn = null;
        return res;
    }

    public static <S extends Suspend, T, Out, In> In pause(S scope, final Function<Continuation<S, T>, Out> f) throws S {
        ValuedContinuation<S, ?, Out, In> c = (ValuedContinuation<S, ?, Out, In>) Continuation.suspend(scope, new CalledCC<S, T>() {
            @Override
            public Continuation<S, T> suspended(Continuation<S, T> c) {
                ((ValuedContinuation<S, ?, Out, In>) c).pauseOut = f.apply(c);
                return null;
            }
        });
        In res = c.pauseIn;
        c.pauseIn = null;
        return res;
    }

    public Out getPauseValue() {
        return pauseOut;
    }
}
