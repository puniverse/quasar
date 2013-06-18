/*
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.concurrent.util.UtilUnsafe;
import jsr166e.ForkJoinTask;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
public abstract class ParkableForkJoinTask<V> extends ForkJoinTask<V> {
    public static final FlightRecorder RECORDER = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;
    public static final Park PARK = new Park();
    public static final int RUNNABLE = 0;
    public static final int LEASED = 1;
    public static final int PARKED = -1;
    public static final int PARKING = -2;
    //
    static final ThreadLocal<ParkableForkJoinTask<?>> current = new ThreadLocal<ParkableForkJoinTask<?>>();
    private volatile int state;
    private volatile Object blocker;

    public ParkableForkJoinTask() {
        state = RUNNABLE;
    }

    protected static ParkableForkJoinTask<?> getCurrent() {
        return current.get();
    }

    @Override
    protected boolean exec() {
        final ParkableForkJoinTask<?> previousCurrent = current.get();
        current.set(this);
        try {
            return doExec();
        } finally {
            current.set(previousCurrent);
        }
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
        return blocker;
    }

    protected void setBlocker(Object blocker) {
        this.blocker = blocker;
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

    protected boolean park1(Object blocker) throws Exception {
        int newState;
        for (;;) {
            final int _state = getState();
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

            if (compareAndSetState(_state, newState)) {
                if (Debug.isDebug())
                    record("park", "current: %s - %s -> %s", this, _state, newState);
                break;
            }
        }
        if (newState == PARKING) {
            this.blocker = blocker;
            parking(false);
            throwPark(false);
            return true;
        } else
            return false;
    }

    public void unpark() {
        if (isDone())
            return;
        int newState;
        for (;;) {
            final int _state = getState();
            switch (_state) {
                case RUNNABLE:
                    newState = LEASED;
                    break;
                case PARKED:
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

            if (compareAndSetState(_state, newState)) {
                if (Debug.isDebug())
                    record("unpark", "current: %s - %s -> %s", this, _state, newState);
                break;
            }
        }
        if (newState == RUNNABLE)
            submit();
    }

    protected boolean tryUnpark() {
        return compareAndSetState(PARKED, RUNNABLE);
    }

    protected void yield1() throws Exception {
        parking(true);
        onParked(true);
        throwPark(true);
    }

    protected void submit() {
        fork();
    }

    protected int getState() {
        return state;
    }

    boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    @Override
    public String toString() {
        return "ParkableForkJoinTask@" + Integer.toHexString(System.identityHashCode(this));
    }

    protected boolean isRecording() {
        return RECORDER != null;
    }

    static void record(String method, String format) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("BlockableForkJoinTask", method, format, null));
    }

    static void record(String method, String format, Object arg1) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("BlockableForkJoinTask", method, format, new Object[]{arg1}));
    }

    static void record(String method, String format, Object arg1, Object arg2) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("BlockableForkJoinTask", method, format, new Object[]{arg1, arg2}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("BlockableForkJoinTask", method, format, new Object[]{arg1, arg2, arg3}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("BlockableForkJoinTask", method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("BlockableForkJoinTask", method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }
    private static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long stateOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset(ParkableForkJoinTask.class.getDeclaredField("state"));
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
}
