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
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * This class is not thread safe. All runs of the continuation must be performed on the same strand, or some synchronization measures need be taken.
 * If you need a continuation that can be triggered by multiple strands, please use a fiber.
 * @author pron
 */
public abstract class Continuation<S extends Suspend, T> implements Runnable, Serializable, Cloneable {
    public static final int DEFAULT_STACK_SIZE = 8;
    private static final boolean ALLOW_CLONING = true;
    private static final boolean verifyInstrumentation = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation");
    protected static final FlightRecorder flightRecorder = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;

    private static final ThreadLocal<Continuation> currentContinuation = new ThreadLocal<>();

    private final Class<S> scope;
    private Callable<T> target;
    private Stack stack;
    private ThreadData threadData;
    private boolean done;
    private T result;
    private Continuation parent;
    private boolean recursive;
    // From this point down, fields are necessary only for cloning
    private IdentityHashMap<Continuation<?, ?>, Continuation<?, ?>> children;

    private static final ThreadLocal<CalledCC> calledcc = new ThreadLocal<>();

    @SuppressWarnings("LeakingThisInConstructor")
    public Continuation(Class<S> scope, boolean detached, int stackSize, Callable<T> target) {
        if (scope == null)
            throw new IllegalArgumentException("Scope is null");
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

    /**
     * Public methods must use self
     * @return
     */
    protected Continuation<S, T> self() {
        return getClone();
    }

    @Override
    public Continuation<S, T> clone() throws CloneNotSupportedException {
        if (!ALLOW_CLONING)
            throw new CloneNotSupportedException();

        if (threadData != null)
            throw new UnsupportedOperationException("Cannot clone a detached continuation");
        if (getCurrentContinuation() == this)
            throw new IllegalStateException("Cannot clone a continuation while running in it: " + getCurrentContinuation());
        try {
            System.err.println("CLONE: " + this);
            Continuation<S, T> o = (Continuation<S, T>) super.clone();
            o.recursive = false;
            if (stack != null) {
                o.stack = new Stack(o, stack);
                Object pc = stack.getSuspendedContext();
                o.stack.setPauseContext(pc == this ? o : pc);
            }
            if (children != null) {
                o.children = (IdentityHashMap<Continuation<?, ?>, Continuation<?, ?>>) children.clone();
                for (Map.Entry e : o.children.entrySet())
                    e.setValue(((Continuation<?, ?>) e.getValue()).clone());
            }
            System.err.println("CLONE: " + this + " -> " + o);

            return o;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    private Continuation<S, T> getClone() {
        if (!ALLOW_CLONING)
            return this;
        final Continuation<?, ?> p = getCurrentContinuation();
        if (p == null)
            return this;
        if (p.children == null)
            return this;
        final Continuation<S, T> cl = (Continuation<S, T>) p.children.get(this);
        return cl != null ? cl : this;
    }

    private void addChild(Continuation<?, ?> child) {
        System.err.println("ADD_CHILD: " + this + ": " + child);
        if (children == null)
            children = new IdentityHashMap<>();
        children.putIfAbsent(child, child);
    }

    private void removeChild(Continuation<?, ?> child) {
        if (children != null)
            children.remove(child);
    }

    private void clear() {
        this.stack = null;
        this.target = null;
        this.threadData = null;
    }

    Stack getStack() {
        return stack;
    }

    ThreadData getThreadData() {
        return threadData;
    }

    public boolean isDone() {
        boolean res = isDone0();
        System.err.println("IS_DONE: " + this + " " + res);
        return res;
    }

    public boolean isDone0() {
        return self().done;
    }

    public T getResult() {
        return self().result;
    }

    @Override
    public int hashCode() {
        return getClone().hashCode0();
    }

    private int hashCode0() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Continuation))
            return false;
        return getClone().equals0(((Continuation<?, ?>) o).getClone());
    }

    private boolean equals0(Object o) {
        return o == this;
    }

    @Override
    public String toString() {
        return getClone().toString0();
    }

    private String toString0() {
        return getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(this))
                + "{scope: " + scope.getSimpleName() + " stack: " + stack + " done: " + isDone0() + '}';
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
        // return suspend(scope, null);
        if (verifyInstrumentation)
            verifySuspend();
        throw scope;
    }

    /**
     * Subclasses calling this method must call it explicitly so, {@code Continuation.suspend}, and not simply {@code suspend}.
     */
    public static <S extends Suspend, T> Continuation<S, T> suspend(S scope, CalledCC<S> ccc) throws S {
        if (verifyInstrumentation)
            verifySuspend();
        calledcc.set(ccc);
        throw scope;
    }

    @Suspendable // may suspend enclosing continuations/fiber
    @Override
    public final void run() {
        getClone().run1();
    }

    @Suspendable // may suspend enclosing continuations/fiber
    private void run1() {
        /*
         * We must keep c in the object on the heap because run0 may pause on an outer scope, and we need to preserve the 
         * current continuation for when we resume (on the outer scope).
         * Also, it's better, because it's really part of this object's state.
         */
        run0();
        if (recursive)
            return;
        recursive = true;
        System.err.println("RECURSIVE TRUE: " + this);
        try {
            CalledCC ccc;
            while ((ccc = calledcc.get()) != null) {
                calledcc.set(null);
                ccc.suspended(this);
            }
            // System.err.println("RRRRRRRRRRR: " + c + " :: " + this);
        } finally {
            System.err.println("RECURSIVE FALSE: " + this);
            recursive = false;
        }
    }

    private void run0() {
        System.err.println("RUN: " + this);
        if (!ALLOW_CLONING && isDone())
            throw new IllegalStateException("Continuation terminated: " + this);
        Throwable susScope = null;
        final Thread currentThread = threadData != null ? Thread.currentThread() : null;
        prepare(currentThread);
        try {
            T res = target.call();
            done(res, null);
        } catch (Suspend s) {
            susScope = s;
            verifyScope(s);
        } catch (Throwable t) {
            if (isContinuationScope(t) || isFiberScope(t)) {
                susScope = t;
                throw t;
            }

            done(null, t);
            throw t;
        } finally {
            restore(susScope, currentThread);
        }
    }

    private boolean isScope(Throwable s) {
        return scope.isInstance(s);
    }

    private static boolean isFiberScope(Throwable s) {
        return s instanceof SuspendExecution || s instanceof RuntimeSuspendExecution;
    }

    private static boolean isContinuationScope(Throwable s) {
        return s instanceof Suspend;
    }

    protected void verifyScope(Suspend s) {
        if (!isScope(s))
            throw s;
    }

    protected void prepare(Thread currentThread) {
        System.err.println("PREPARE: " + this);
        this.parent = getCurrentContinuation();
//        if (threadData != null & parent != null)
//            throw new IllegalStateException("Cannot run a detached continuation nested within another continuation: " + parent);

        prepareStack();
        currentContinuation.set(this);
        if (threadData != null) {
            record(2, "Continuation", "prepare", "threadData: %s", threadData);
            threadData.installDataInThread(currentThread);
        }
        calledcc.set(null);
    }

    private void restore(Throwable susScope, Thread currentThread) {
        final boolean inScope = isScope(susScope);
        try {
            System.err.println("RESTORE: " + this + " " + inScope + " -> " + parent);
            if (stack != null) {
                if (!inScope)
                    stack.setPauseContext(null);
                restoreStack(susScope);
            }
            if (threadData != null) {
                record(2, "Continuation", "restore", "threadData: %s", threadData);
                threadData.restoreThreadData(currentThread);
            }
        } finally {
            currentContinuation.set(parent);
            this.parent = null;
        }
    }

    private void prepareStack() {
        if (parent != null && stack.getSuspendedContext() == null)
            stack.setPauseContext(parent.getStack().getSuspendedContext());
    }

    private void restoreStack(Throwable susScope) {
        final boolean inScope = isScope(susScope);
        if (ALLOW_CLONING && parent != null) {
            if (!inScope && susScope != null && isContinuationScope(susScope)) { // special treatment for fibers; we don't want them bothered
                System.err.println("RESTORE EMBEDDED OUTER: " + stack + " -> " + parent.getStack());
                parent.addChild(this);
            }
        }

        if (inScope)
            stack.setPauseContext(this);

        stack.resumeStack();
    }

    private void done(T result, Throwable t) {
        System.err.println("DONE: " + this + " :: " + t);
        if (t != null)
            t.printStackTrace(System.err);

        clear();
        this.done = true;
        this.result = result;
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
