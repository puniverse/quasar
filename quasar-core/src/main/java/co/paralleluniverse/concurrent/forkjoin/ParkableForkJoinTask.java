/*
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.concurrent.forkjoin;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.common.util.UtilUnsafe;
import co.paralleluniverse.concurrent.util.ThreadAccess;
import co.paralleluniverse.fibers.Fiber;
import jsr166e.ForkJoinPool;
import jsr166e.ForkJoinTask;
import jsr166e.ForkJoinWorkerThread;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
public abstract class ParkableForkJoinTask<V> extends ForkJoinTask<V> {
    public static final FlightRecorder RECORDER = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;
    public static final boolean CAPTURE_UNPARK_STACK = Debug.isDebug() || Boolean.getBoolean("co.paralleluniverse.fibers.captureUnparkStackTrace");
    //public static final Object EMERGENCY_UNBLOCKER = new Object();
    public static final Park PARK = new Park();
    public static final int RUNNABLE = 0;
    public static final int LEASED = 1;
    public static final int PARKED = -1;
    public static final int PARKING = -2;
    //
    private final DummyRunnable taskRef = new DummyRunnable(this);
    private volatile int state; // The state field is updated while enforcing memory consistency. This beats the "don't re-fork" rule (see Javadoc for ForkJoinTask.fork())
    private Object blocker;
    private ParkableForkJoinTask enclosing;
    private boolean parkExclusive;
    private Object unparker;
    private StackTraceElement[] unparkStackTrace;

    public ParkableForkJoinTask() {
        state = RUNNABLE;
    }

    public static ParkableForkJoinTask<?> getCurrent() {
        ParkableForkJoinTask ct = getCurrent1();
        if (ct == null && Thread.currentThread() instanceof ForkJoinWorkerThread) { // false in tests
            Fiber f = Fiber.currentFiber();
            if (f != null)
                ct = (ParkableForkJoinTask) f.getTask();
        }
        return ct;
    }

    @Override
    protected boolean exec() {
        final Thread currentThread = Thread.currentThread();
        final Object oldTarget = getTarget(currentThread);
        this.enclosing = fromTarget(oldTarget);
        this.parkExclusive = false;
        this.blocker = null;
        setCurrent(this);
        try {
            return doExec();
        } finally {
            setTarget(currentThread, oldTarget); // can't use enclosing for the same reason can't nullify enclosing. See below.
            //enclosing = null; -- can't nullify enclosing here, because by the time we get here, his task may have been re-scheduled and enclosing re-set
        }
    }

    static void setCurrent(ParkableForkJoinTask<?> task) {
        setTarget(Thread.currentThread(), task != null ? task.taskRef : null);
    }

    static ParkableForkJoinTask<?> fromTarget(Object target) {
        if (target instanceof DummyRunnable)
            return ((DummyRunnable) target).task;
        return null;
    }

    static ParkableForkJoinTask<?> getCurrent1() {
        return fromTarget(getTarget(Thread.currentThread()));
    }

    public static void setTarget(Thread thread, Object target) {
        if (thread instanceof ExtendedForkJoinWorkerThread)
            ((ExtendedForkJoinWorkerThread) thread).setTarget(target);
        else
            ThreadAccess.setTarget(thread, (Runnable) target);
    }

    public static Object getTarget(Thread thread) {
        if (thread instanceof ExtendedForkJoinWorkerThread)
            return ((ExtendedForkJoinWorkerThread) thread).getTarget();
        else
            return ThreadAccess.getTarget(thread);
    }

    boolean doExec() {
        try {
            onExec();
            boolean res = exec1();
            onCompletion(res);
            return res;
        } catch (Park park) {
            return false;
        } catch (Throwable t) {
            onException(t);
            return true;
        }
    }

    protected abstract boolean exec1();

    public Object getBlocker() {
        if (state != PARKED)
            return null; // volatile read
        return blocker;
    }

    ParkableForkJoinTask getEnclosing() {
        return enclosing;
    }

    protected void onExec() {
        if (Debug.isDebug())
            record("doExec", "executing %s", this);
    }

    protected void onCompletion(boolean res) {
        record("doExec", "done normally %s", this, Boolean.valueOf(res));
    }

    protected void onException(Throwable t) {
        record("doExec", "exception in %s - %s, %s", this, t, t.getStackTrace());
        throw Exceptions.rethrow(t);
    }

    protected void parking(boolean yield) {
        doPark(yield);
    }

    protected void onParked(boolean yield) {
        if (Debug.isDebug())
            record("doExec", "parked " + (yield ? "(yield)" : "(park)") + " %s", this);
    }

    protected Object getUnparker() {
        return unparker;
    }

    protected StackTraceElement[] getUnparkStackTrace() {
        return unparkStackTrace;
    }

    protected void doPark(boolean yield) {
        if (yield)
            submit();
        else
            this.state = PARKED;
        onParked(yield);
    }

    protected void throwPark(boolean yield) throws Exception {
        throw PARK;
    }

    protected boolean park(Object blocker) throws Exception {
        return park(blocker, false);
    }

    protected boolean park(Object blocker, boolean exclusive) throws Exception {
        int newState;
        int _state;
        do {
            _state = getState();
            switch (_state) {
                case LEASED:
                    newState = RUNNABLE;
                    break;
                case RUNNABLE:
                    newState = PARKING;
                    break;
                case PARKING:
                case PARKED:
                    throw new AssertionError("Illegal task state: " + _state);
                default:
                    throw new AssertionError("Unknown task state: " + _state);
            }
        } while (!compareAndSetState(_state, newState));

        if (Debug.isDebug())
            record("park", "current: %s - %s -> %s (blocker: %s)", this, _state, newState, blocker);
        if (newState == PARKING) {
            this.blocker = blocker;
            this.parkExclusive = exclusive;
            parking(false);
            throwPark(false);
            return true;
        } else
            return false;
    }

    public void unpark() {
        unpark(null);
    }

    public void unpark(Object unblocker) {
        unpark(null, unblocker);
    }

    public void unpark(ForkJoinPool fjPool, Object unblocker) {
        if (isDone())
            return;

        int newState;
        int _state;
        for (;;) {
            _state = getState();
            switch (_state) {
                case RUNNABLE:
                    newState = LEASED;
                    break;
                case PARKED:
                    if (parkExclusive && blocker != unblocker)
                        return;
                    newState = RUNNABLE;
                    break;
                case PARKING:
                    continue; // spin and wait
                case LEASED:
                    if (Debug.isDebug())
                        record("unpark", "current: %s - %s. return.", this, _state);
                    return;
                default:
                    throw new AssertionError("Unknown task state: " + _state);
            }
            if (compareAndSetState(_state, newState))
                break;
        }

        if (Debug.isDebug())
            record("unpark", "current: %s - %s -> %s", this, _state, newState);
        if (newState == RUNNABLE) {
            this.unparker = unblocker;
            if (CAPTURE_UNPARK_STACK)
                this.unparkStackTrace = Thread.currentThread().getStackTrace();
            if (fjPool != null)
                submit(fjPool);
            else
                submit();
        }
    }

    protected boolean tryUnpark(Object unblocker) {
        boolean res = compareAndSetState(PARKED, RUNNABLE);
        return res;
    }

    protected void yield() throws Exception {
        parking(true);
        onParked(true);
        throwPark(true);
    }

    protected void submit() {
        assert Thread.currentThread() instanceof jsr166e.ForkJoinWorkerThread;
        fork();
    }

    private void submit(ForkJoinPool fjPool) {
        if (ForkJoinTask.getPool() == fjPool)
            fork();
        else
            fjPool.submit(this);
    }

    protected int getState() {
        return state;
    }

    protected void setState(int state) {
        this.state = state;
    }

    boolean compareAndSetState(int expect, int update) {
        return UNSAFE.compareAndSwapInt(this, stateOffset, expect, update);
    }

    @Override
    public String toString() {
        return "ParkableForkJoinTask@" + Integer.toHexString(System.identityHashCode(this));
    }

    protected boolean isRecording() {
        return RECORDER != null;
    }

    public static void record(String method, String format) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ParkableForkJoinTask", method, format, null));
    }

    public static void record(String method, String format, Object arg1) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ParkableForkJoinTask", method, format, new Object[]{arg1}));
    }

    public static void record(String method, String format, Object arg1, Object arg2) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ParkableForkJoinTask", method, format, new Object[]{arg1, arg2}));
    }

    public static void record(String method, String format, Object arg1, Object arg2, Object arg3) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ParkableForkJoinTask", method, format, new Object[]{arg1, arg2, arg3}));
    }

    public static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ParkableForkJoinTask", method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    public static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ParkableForkJoinTask", method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }
    private static final Unsafe UNSAFE = UtilUnsafe.getUnsafe();
    private static final long stateOffset;

    static {
        try {
            stateOffset = UNSAFE.objectFieldOffset(ParkableForkJoinTask.class.getDeclaredField("state"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    public static class Park extends Error {
        private Park() {
            super(null, null, false, false);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private static final class DummyRunnable implements Runnable {
        final ParkableForkJoinTask task;

        public DummyRunnable(ParkableForkJoinTask task) {
            this.task = task;
        }

        @Override
        public void run() {
            throw new RuntimeException("This method shouldn't be run. This object is a placeholder.");
        }
    }
}
