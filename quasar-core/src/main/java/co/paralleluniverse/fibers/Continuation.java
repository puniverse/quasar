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

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.SystemProperties;
import static co.paralleluniverse.fibers.Fiber.checkInstrumentation;
import co.paralleluniverse.strands.Strand;
import java.io.Serializable;

/**
 * This class is not thread safe. All runs of the continuation must be performed on the same strand.
 * If you need a continuation that can be triggered by multiple strands, please use a fiber.
 * @author pron
 */
public abstract class Continuation<S extends Suspend, T> implements Serializable, Cloneable {
    public static final int DEFAULT_STACK_SIZE = 8;
    private static final boolean verifyInstrumentation = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation");
    protected static final FlightRecorder flightRecorder = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;

    private static final ThreadLocal<Continuation> currentContinuation = new ThreadLocal<>();

    private final Class<S> scope;
    private final Continuation parent;
    private final Callable<T> target;
    private Stack stack;
//    private Stack tmpStack;
//    private int embeddedSP = -1;
//    private boolean copiedEmbedded;
    final ThreadData threadData;
    private boolean done;
    private T result;

    private static final ThreadLocal<CalledCC> calledcc = new ThreadLocal<>();

    public Continuation(Class<S> scope, boolean detached, int stackSize, Callable<T> target) {
        if (scope == null)
            throw new IllegalArgumentException("Scope is null");
        this.parent = getCurrentContinuation();
        if (detached & parent != null)
            throw new IllegalStateException("Cannot create a detached continuation nested within another continuation: " + parent);
        this.target = target;
        this.stack = new Stack(this, stackSize > 0 ? stackSize : DEFAULT_STACK_SIZE);
        this.scope = scope;
        this.threadData = detached ? new ThreadData(Thread.currentThread()) : null;

        System.err.println("INIT: " + this);
    }

    public Continuation(Class<S> scope, boolean detached, Callable<T> target) {
        this(scope, detached, 0, target);
    }

    public Continuation(Class<S> scope, Callable<T> target) {
        this(scope, false, 0, target);
    }

    Stack getStack() {
        return stack;
    }

    @Override
    public Continuation<S, T> clone() {
        try {
            if (threadData != null)
                throw new UnsupportedOperationException("Cannot clone a detached continuation");
            Continuation<S, T> o = (Continuation<S, T>) super.clone();
            o.stack = new Stack(o, stack);
            o.stack.setPauseContext(stack.getPausedContext() == this ? o : stack.getPausedContext());
            // System.err.println("CLONE: " + this + " -> " + o + " PC: " + o.stack.getPausedContext());
            return o;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "{scope: " + scope.getSimpleName() + " stack: " + stack + " parent: " + parent + '}';
    }

    static Continuation getCurrentContinuation() {
        return currentContinuation.get();
    }

    static Continuation getDetachedContinuation() {
        for (Continuation c = getCurrentContinuation(); c != null; c = c.parent) {
            if (c.threadData != null)
                return c;
        }
        return null;
    }

    static void suspend0(Suspend suspend) {
        throw suspend;
    }

    /**
     * Subclasses calling this method must call it explicitly so, {@code Continuation.suspend}, and not simply {@code suspend}.
     */
    public static <S extends Suspend> Continuation<S, ?> suspend(S scope) throws S {
        if (verifyInstrumentation)
            verifySuspend();
        if (true)
            throw scope;
        return null;
    }

    /**
     * Subclasses calling this method must call it explicitly so, {@code Continuation.suspend}, and not simply {@code suspend}.
     */
    public static <S extends Suspend, T> Continuation<S, T> suspend(S scope, CalledCC<S> ccc) throws S {
        if (verifyInstrumentation)
            verifySuspend();
        calledcc.set(ccc);
        if (true)
            throw scope;
        return null;
    }

    @Suspendable // may suspend enclosing continuations/fiber
    public final Continuation<S, T> go() {
        Continuation<S, T> c = this, res;
        do {
            res = c;
            CalledCC ccc = c.run0();
            c = ccc != null ? ccc.suspended(c) : null;
        } while (c != null);
        return res;
    }

    private CalledCC run0() {
        // System.err.println("RUN: " + this);
        if (isDone())
            throw new IllegalStateException("Continuation terminated: " + this);
        final Thread currentThread = threadData != null ? Thread.currentThread() : null;
        final Continuation<?, ?> prev = prepare0(currentThread);
        try {
            result = target.call();
            done0(null);
            return null;
        } catch (Suspend s) {
            verifyScope(s);

            final CalledCC ccc = calledcc.get();
            if (ccc != null)
                calledcc.set(null);

            suspendStack();

            return ccc;
        } catch (Throwable t) {
            if (t instanceof Suspend || t instanceof SuspendExecution || t instanceof RuntimeSuspendExecution)
                throw t;

            // System.err.println("EXCEPTION: " + t);
            // t.printStackTrace(System.err);

            done0(t);
            throw t;
        } finally {
            restore0(currentThread, prev);
        }
    }

    protected void verifyScope(Suspend s) {
        if (!scope.isInstance(s))
            throw s;
    }

    protected Continuation<?, ?> prepare0(Thread currentThread) {
        // System.err.println("PREPARE: " + this);
        Continuation<?, ?> prev = currentContinuation.get();
        prepareStack(prev);
        currentContinuation.set(this);
        if (threadData != null) {
            record(2, "Continuation", "prepare", "threadData: %s", threadData);
            threadData.installDataInThread(currentThread);
        }
        calledcc.set(null);
        prepare();
        return prev;
    }

    private void restore0(Thread currentThread, Continuation<?, ?> prev) {
        try {
            // System.err.println("RESTORE: " + prev);
            if (stack != null) {
                if (stack.getPausedContext() != this)
                    stack.setPauseContext(null);
                restoreStack(prev);
            }
            if (threadData != null) {
                record(2, "Continuation", "restore", "threadData: %s", threadData);
                threadData.restoreThreadData(currentThread);
            }
            restore();
        } finally {
            currentContinuation.set(prev);
        }
    }

//    private boolean isEmbedded(Continuation<?, ?> prev) {
//        return parent != null && prev == parent;
//    }
    private void prepareStack(Continuation<?, ?> prev) {
//        if (isEmbedded(prev)) {
//            tmpStack = stack;
//            stack = prev.stack;
//            assert embeddedSP < 0 || embeddedSP == stack.capturePosition() : embeddedSP + " :: " + stack.capturePosition();
//            embeddedSP = stack.capturePosition();
//            if (copiedEmbedded) {
//                tmpStack.putTop(stack, embeddedSP);
//                copiedEmbedded = false;
//            }
//
//            // System.err.println("PREPARE EMBEDDED: " + stack);
//        }
        if (prev != null && stack.getPausedContext() == null)
            stack.setPauseContext(prev);
    }

    private void suspendStack() {
//        if (embeddedSP >= 0) {
//            tmpStack.moveTop(stack, embeddedSP);
//            embeddedSP = -1;
//            copiedEmbedded = true;
//            tmpStack.setPauseContext(this);
//        }
        stack.setPauseContext(this);
    }

    private void restoreStack(Continuation<?, ?> prev) {
//        if (isEmbedded(prev)) {
//            // System.err.println("RESTORE EMBEDDED: " + stack + " -> " + tmpStack);
//            stack = tmpStack;
//            tmpStack = null;
//        }

        stack.resumeStack();
    }

    private void done0(Throwable t) {
        // System.err.println("DONE: " + this + " :: " + t);
//        tmpStack = null;
        stack = null;
        done = true;
        done();
    }

    protected void prepare() {
    }

    protected void restore() {
    }

    protected void done() {
    }

    public boolean isDone() {
        return done;
    }

    public T getResult() {
        return result;
    }

    private static Continuation<?, ?> verifySuspend() {
        return verifySuspend(verifyCurrent());
    }

    static Continuation<?, ?> verifySuspend(Continuation<?, ?> current) {
        if (verifyInstrumentation)
            checkInstrumentation();
        return current;
    }

    private static Continuation<?, ?> verifyCurrent() {
        Continuation<?, ?> current = getCurrentContinuation();
        if (current == null)
            throw new IllegalStateException("Not called on a continuation (current strand: " + Strand.currentStrand() + ")");
        return current;
    }

    //<editor-fold defaultstate="collapsed" desc="Recording">
    /////////// Recording ///////////////////////////////////
    protected final boolean isRecordingLevel(int level) {
        if (!Debug.isDebug())
            return false;
        final FlightRecorder.ThreadRecorder recorder = flightRecorder.get();
        if (recorder == null)
            return false;
        return recorder.recordsLevel(level);
    }

    protected final void record(int level, String clazz, String method, String format) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4, arg5);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    protected final void record(int level, String clazz, String method, String format, Object... args) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, args);
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, null));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4, arg5, arg6}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4, arg5, arg6, arg7}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object... args) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, args));
    }

    private static FlightRecorderMessage makeFlightRecorderMessage(FlightRecorder.ThreadRecorder recorder, String clazz, String method, String format, Object[] args) {
        return new FlightRecorderMessage(clazz, method, format, args);
        //return ((FlightRecorderMessageFactory) recorder.getAux()).makeFlightRecorderMessage(clazz, method, format, args);
    }
    //</editor-fold>
}
