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
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.common.util.UtilUnsafe;
import static co.paralleluniverse.fibers.FiberTask.*;
import co.paralleluniverse.fibers.instrument.DontInstrument;
import co.paralleluniverse.strands.SettableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
class RunnableFiberTask<V> implements Runnable, FiberTask {
    public static final FlightRecorder RECORDER = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;
    public static final boolean CAPTURE_UNPARK_STACK = Debug.isDebug() || SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.captureUnparkStackTrace");
    //public static final Object EMERGENCY_UNBLOCKER = new Object();
    //
//    private final DummyRunnable taskRef = new DummyRunnable(this);
    private final Executor executor;
    private final Fiber<V> fiber;
    private volatile int state; // The state field is updated while enforcing memory consistency. This beats the "don't re-fork" rule (see Javadoc for ForkJoinTask.fork())
    private Object blocker;
//    private RunnableFiberTask enclosing;
    private boolean parkExclusive;
    private Object unparker;
    private StackTraceElement[] unparkStackTrace;
    private final SettableFuture<V> future;

    public RunnableFiberTask(Fiber<V> fiber, Executor executor) {
        this.executor = executor;
        this.fiber = fiber;
        this.state = RUNNABLE;
        this.future = Fiber.USE_VAL_FOR_RESULT ? null : new SettableFuture<V>();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    @Suspendable
    public Object get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    @Suspendable
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public void run() {
        doExec();
    }

    @Override
    public boolean doExec() {
        this.parkExclusive = false;
        this.blocker = null;
        try {
            onExec();
            boolean res = fiber.exec();
            onCompletion(res);
            return res;
        } catch (Throwable t) {
            onException(t);
            if (future != null)
                future.setException(t);
            return true;
        }
    }

    @Override
    public Object getBlocker() {
        if (state != PARKED)
            return null; // volatile read
        return blocker;
    }

//    RunnableFiberTask getEnclosing() {
//        return enclosing;
//    }
    protected void onExec() {
        if (Debug.isDebug())
            record("doExec", "executing %s", this);
    }

    protected void onCompletion(boolean res) {
        if (res) {
            fiber.onCompletion();
            if (future != null)
                future.set(fiber.getResult());
        }
    }

    protected void onException(Throwable t) {
        fiber.onException(t);
    }

    protected void parking(boolean yield) {
    }

    protected void onParked(boolean yield) {
        if (Debug.isDebug())
            record("doExec", "parked " + (yield ? "(yield)" : "(park)") + " %s", this);
        fiber.onParked();
    }

    @Override
    public Object getUnparker() {
        return unparker;
    }

    @Override
    public StackTraceElement[] getUnparkStackTrace() {
        return unparkStackTrace;
    }

    @Override
    public void doPark(boolean yield) {
        if (yield)
            submit();
        else
            this.state = PARKED;
        onParked(yield);
    }

    @DontInstrument
    protected void throwPark(boolean yield) throws SuspendExecution {
        throw yield ? SuspendExecution.YIELD : SuspendExecution.PARK;
    }

    protected boolean park(Object blocker) throws Exception {
        return park(blocker, false);
    }

    @DontInstrument
    @Override
    public boolean park(Object blocker, boolean exclusive) throws SuspendExecution {
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

    @Override
    public void unpark() {
        unpark(null);
    }

    @Override
    public void unpark(Object unblocker) {
        if (fiber.isDone())
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
                    if (parkExclusive & unblocker != blocker & unblocker != EMERGENCY_UNBLOCKER)
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
            submit();
        }
    }

    @Override
    public boolean tryUnpark(Object unblocker) {
        boolean res = compareAndSetState(PARKED, RUNNABLE);
        return res;
    }

    @DontInstrument
    @Override
    public void yield() throws SuspendExecution {
        parking(true);
        onParked(true);
        throwPark(true);
    }

    @Override
    public void submit() {
        executor.execute(this);
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public void setState(int state) {
        this.state = state;
    }
    
    boolean compareAndSetState(int expect, int update) {
        return UNSAFE.compareAndSwapInt(this, stateOffset, expect, update);
    }

    @Override
    public String toString() {
        return super.toString() + "(Fiber@" + fiber.getId() + ')';
    }

    protected boolean isRecording() {
        return RECORDER != null;
    }

    public static void record(String method, String format) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("RunnableFiberTask", method, format, null));
    }

    public static void record(String method, String format, Object arg1) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("RunnableFiberTask", method, format, new Object[]{arg1}));
    }

    public static void record(String method, String format, Object arg1, Object arg2) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("RunnableFiberTask", method, format, new Object[]{arg1, arg2}));
    }

    public static void record(String method, String format, Object arg1, Object arg2, Object arg3) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("RunnableFiberTask", method, format, new Object[]{arg1, arg2, arg3}));
    }

    public static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("RunnableFiberTask", method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    public static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("RunnableFiberTask", method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }
    private static final Unsafe UNSAFE = UtilUnsafe.getUnsafe();
    private static final long stateOffset;

    static {
        try {
            stateOffset = UNSAFE.objectFieldOffset(RunnableFiberTask.class.getDeclaredField("state"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

//    private static final class DummyRunnable implements Runnable {
//        final RunnableFiberTask task;
//
//        public DummyRunnable(RunnableFiberTask task) {
//            this.task = task;
//        }
//
//        @Override
//        public void run() {
//            throw new RuntimeException("This method shouldn't be run. This object is a placeholder.");
//        }
//    }
}
