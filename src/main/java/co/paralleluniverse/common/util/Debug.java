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
package co.paralleluniverse.common.util;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author pron
 */
public class Debug {
    private static final boolean debugMode = Boolean.getBoolean("co.paralleluniverse.debugMode");
    private static final String FLIGHT_RECORDER_DUMP_FILE = System.getProperty("co.paralleluniverse.flightRecorderDumpFile");
    private static final FlightRecorder flightRecorder = (Boolean.getBoolean("co.paralleluniverse.debugMode") && Boolean.getBoolean("co.paralleluniverse.globalFlightRecorder") ? new FlightRecorder("PUNIVERSE-FLIGHT-RECORDER") : null);
    private static boolean recordStackTraces = false;
    private static final boolean assertionsEnabled;
    private static boolean unitTest;
    private static final AtomicBoolean requestShutdown = new AtomicBoolean(false);
    private static final AtomicBoolean fileDumped = new AtomicBoolean(false);

    static {
        boolean ea = false;
        assert (ea = true);
        assertionsEnabled = ea;
        unitTest = false;
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stack) {
            if (ste.getClassName().startsWith("org.junit") || ste.getClassName().startsWith("junit.framework")) {
                unitTest = true;
                break;
            }
        }

        if (debugMode) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    if (requestShutdown.get())
                        dumpRecorder();
                }
            });
        }
    }

    public static boolean isDebug() {
        return debugMode;
    }

    public static boolean isAssertionsEnabled() {
        return assertionsEnabled;
    }

    public static boolean isRecordStackTraces() {
        return recordStackTraces;
    }

    public static boolean isUnitTest() {
        return unitTest;
    }

    public static void setRecordStackTraces(boolean recordStackTraces) {
        Debug.recordStackTraces = recordStackTraces;
    }

    public static String getDumpFile() {
        return FLIGHT_RECORDER_DUMP_FILE;
    }

    public static FlightRecorder getGlobalFlightRecorder() {
        return flightRecorder;
    }

    public static void exit(int code) {
        if (flightRecorder != null) {
            flightRecorder.record(1, "DEBUG EXIT REQUEST");
            flightRecorder.stop();
        }

        if (requestShutdown.compareAndSet(false, true)) {
            System.err.println("SHUTTING DOWN THE VM.");
            System.exit(code);
        }
    }

    private static void dumpRecorder() {
        if (isDebug()) {
            final String fileName = getDumpFile();
            if (fileName != null && !fileName.trim().equals("")) {
                if (fileDumped.compareAndSet(false, true))
                    dumpRecorder(fileName);
            } else
                System.err.println("NO ERROR LOG FILE SPECIFIED.");
        }
    }

    private static void dumpRecorder(String filename) {
        if (flightRecorder != null)
            flightRecorder.dump(filename);
    }

    public static void dumpAfter(final long millis) {
        dumpAfter(millis, FLIGHT_RECORDER_DUMP_FILE);
    }

    public static void dumpAfter(final long millis, final String filename) {
        if(!debugMode)
            return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(millis);
                    dumpRecorder(filename);
                } catch (InterruptedException e) {
                }
            }
        }, "DEBUG").start();
    }

    public interface StackTraceFilter {
        boolean filter(StackTraceElement ste);
    }
    private static final StackTraceFilter UNITTEST_FILTER = new StackTraceFilter() {
        @Override
        public boolean filter(StackTraceElement ste) {
            return !(ste.getClassName().startsWith("org.mockito")
                    || ste.getClassName().startsWith("org.junit")
                    || ste.getClassName().startsWith("org.apache.tools.ant.taskdefs.optional.junit"));
        }
    };

    public static void dumpStack() {
        dumpStack(System.out, new Exception("Stack trace"), UNITTEST_FILTER);
    }

    public static void dumpStack(PrintStream s, Throwable t) {
        dumpStack(s, t, UNITTEST_FILTER);
    }

    public static void dumpStack(PrintStream s, Throwable t, StackTraceFilter filter) {
        synchronized (s) {
            s.println(t);
            StackTraceElement[] trace = t.getStackTrace();
            for (int i = 0; i < trace.length; i++) {
                if (filter.filter(trace[i]))
                    s.println("\tat " + trace[i]);
            }

            Throwable ourCause = t.getCause();
            if (ourCause != null)
                printStackTraceAsCause(s, trace, ourCause, filter);
        }
    }

    /**
     * Print our stack trace as a cause for the specified stack trace.
     */
    private static void printStackTraceAsCause(PrintStream s, StackTraceElement[] causedTrace, Throwable t, StackTraceFilter filter) {
        // assert Thread.holdsLock(s);

        // Compute number of frames in common between this and caused
        StackTraceElement[] trace = t.getStackTrace();
        int m = trace.length - 1, n = causedTrace.length - 1;
        while (m >= 0 && n >= 0 && trace[m].equals(causedTrace[n])) {
            m--;
            n--;
        }
        int framesInCommon = trace.length - 1 - m;

        s.println("Caused by: " + t);
        for (int i = 0; i <= m; i++) {
            if (filter.filter(trace[i]))
                s.println("\tat " + trace[i]);
        }
        if (framesInCommon != 0)
            s.println("\t... " + framesInCommon + " more");

        // Recurse if we have a cause
        Throwable ourCause = t.getCause();
        if (ourCause != null)
            printStackTraceAsCause(s, trace, ourCause, filter);
    }

    private Debug() {
    }
}
