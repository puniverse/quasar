/*
 * Quasar: lightweight threads and actors for the JVM.
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
package co.paralleluniverse.strands;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class ConditionSynchronizer {
    public void await() throws InterruptedException, SuspendExecution {
        if (isRecording())
            record("await", "%s parking", Strand.currentStrand());
        Strand.park(this);
        if (isRecording())
            record("await", "%s awoke", Strand.currentStrand());
        if (Strand.interrupted())
            throw new InterruptedException();
    }

    public long awaitNanos(long timeoutNanos) throws InterruptedException, SuspendExecution {
        final long start = System.nanoTime();
        final long deadline = start + timeoutNanos;

        Strand.parkNanos(this, timeoutNanos);
        if (Strand.interrupted())
            throw new InterruptedException();
        return deadline - System.nanoTime();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        return awaitNanos(unit.toNanos(timeout)) > 0;
    }
    ////////////////////////////
    public static final FlightRecorder RECORDER = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;

    boolean isRecording() {
        return RECORDER != null;
    }

    static void record(String method, String format) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ConditionSynchronizer", method, format, null));
    }

    static void record(String method, String format, Object arg1) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ConditionSynchronizer", method, format, new Object[]{arg1}));
    }

    static void record(String method, String format, Object arg1, Object arg2) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ConditionSynchronizer", method, format, new Object[]{arg1, arg2}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ConditionSynchronizer", method, format, new Object[]{arg1, arg2, arg3}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ConditionSynchronizer", method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("ConditionSynchronizer", method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }
}
