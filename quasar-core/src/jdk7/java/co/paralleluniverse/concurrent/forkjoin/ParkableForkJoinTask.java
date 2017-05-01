/*
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.common.util.UtilUnsafe;
import co.paralleluniverse.concurrent.util.ThreadAccess;
import co.paralleluniverse.fibers.Fiber;
import jsr166e.ForkJoinPool;
import jsr166e.ForkJoinTask;
import jsr166e.ForkJoinWorkerThread;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 *
 * @author pron
 */
public abstract class ParkableForkJoinTask<V> extends ForkJoinTask<V> {
    public static final FlightRecorder RECORDER = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;
    public static final boolean CAPTURE_UNPARK_STACK = Debug.isDebug() || SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.captureUnparkStackTrace");
    public static final Object EMERGENCY_UNBLOCKER = new Object();
    public static final Park PARK = Park.create();
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

    // isolate subclasses from FJTask class (JDK7/8)
    public final void fork1() {
        fork();
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
        if (yield) {
            submit();
        } else {
            int newState;
            int _state;
            loop: do {
                _state = getState();
                switch (_state) {
                    case PARKING:
                        newState = PARKED;
                        break;
                    case RUNNABLE:
                        newState = RUNNABLE;
                        break loop;
                    case LEASED:
                        newState = RUNNABLE;
                        break;
                    default:
                        throw new AssertionError("Illegal task state (a fiber has no chance to enter `doPark` in anything else than `PARKED` or `RESTART`): " + _state);
                }
            } while (!compareAndSetState(_state, newState));

            if (newState == RUNNABLE)
                submit();
        }

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
                    throw new AssertionError("Unexpected task state (fiber parking or parked has no chance to to call `park`): " + _state);
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

    public boolean unpark() {
        return unpark(null);
    }

    public boolean unpark(Object unblocker) {
        return unpark(null, unblocker);
    }

    public boolean unpark(ForkJoinPool fjPool, Object unblocker) {
        if (isDone())
            return false;

        int newState;
        int _state;
        do {
            _state = getState();
            switch (_state) {
                case RUNNABLE:
                    newState = LEASED;
                    break;
                case PARKED:
                    if (parkExclusive & unblocker != blocker & unblocker != EMERGENCY_UNBLOCKER)
                        return false;
                    newState = RUNNABLE;
                    break;
                case PARKING:
                    newState = RUNNABLE; // Represents immediate resume for `doPark`
                    break;
                case LEASED:
                    if (Debug.isDebug())
                        record("unpark", "current: %s - %s. return.", this, _state);
                    return false;
                default:
                    throw new AssertionError("Unknown task state: " + _state);
            }
        } while (!compareAndSetState(_state, newState));

        if (Debug.isDebug())
            record("unpark", "current: %s - %s -> %s", this, _state, newState);

        if (newState == RUNNABLE) {
            this.unparker = unblocker;
            if (CAPTURE_UNPARK_STACK)
                this.unparkStackTrace = Thread.currentThread().getStackTrace();
            if (_state != PARKING) {
                if (fjPool != null)
                    submit(fjPool);
                else
                    submit();
            }
        }

        return _state == PARKED || _state == PARKING; // Actually woken up the fiber
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

    private static final AtomicIntegerFieldUpdater<ParkableForkJoinTask> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(ParkableForkJoinTask.class,"state"); 

    boolean compareAndSetState(int expect, int update) {
        return stateUpdater.compareAndSet(this, expect, update);
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

    public static class Park extends Error {
        private Park() {
            super(null, null, false, false);
        }

        protected Park(String message,Throwable cause) { // Just to provide a means for ParkJava1_6 to call its super-constructor. 
            super(message,cause);
        }
        
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
        
        public static Park create() {
            try {
                return new Park();
            } catch (NoSuchMethodError e) {  // We do not run JDK 1.7 or higher and hence do not have a constructor Error(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) we could call.
                return new ParkJava1_6();    // Hence we need to fallback to a JDK 1.6 implementation.
            }
        }
    }
    
    /*
        Note that there will be only one singleton instance of this class, possibly being thrown by multiple threads simultaneously.
        However, this should not pose any problems, as the Java VM spec does not specify that the "athrow" instruction changes any field of the Throwable object.
        Starting with Java 1.7, exception handler code may call addSuppressed(), but under Java 1.7, this class should not be in use in the first place.
    */
    public static class ParkJava1_6 extends Park {
        private ParkJava1_6() {
            super(null, null);
        }

        public void setStackTrace(StackTraceElement[] stackTrace) {
            throw new AssertionError("Cannot set stack trace "+stackTrace+" to singleton throwable.");
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
