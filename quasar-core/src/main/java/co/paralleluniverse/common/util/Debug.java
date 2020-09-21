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
package co.paralleluniverse.common.util;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.strands.Strand;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author pron
 */
public final class Debug {
    private static final boolean debugMode = SystemProperties.isEmptyOrTrue("co.paralleluniverse.debugMode");
    private static final String FLIGHT_RECORDER_DUMP_FILE = System.getProperty("co.paralleluniverse.flightRecorderDumpFile");
    private static final FlightRecorder flightRecorder = (debugMode && SystemProperties.isEmptyOrTrue("co.paralleluniverse.globalFlightRecorder") ? new FlightRecorder("PUNIVERSE-FLIGHT-RECORDER") : null);
    private static boolean recordStackTraces = false;
    private static final boolean assertionsEnabled;
    private static final boolean unitTest;
    private static final boolean ci;
    private static final boolean debugger;
    private static final AtomicBoolean requestShutdown = new AtomicBoolean();
    private static final Lock dumpLock = new ReentrantLock();
    private static final Condition dumpDone = dumpLock.newCondition();
    private static boolean dumped;

    static {
        boolean ea = false;
        assert (ea = true);
        assertionsEnabled = ea;

        boolean isUnitTest = false;
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stack) {
            if (ste.getClassName().startsWith("org.junit")
                    || ste.getClassName().startsWith("junit.framework")
                    || ste.getClassName().contains("JUnitTestClass")) {
                isUnitTest = true;
                break;
            }
        }
        unitTest = isUnitTest;

        ci = (isEnvTrue("CI") || isEnvTrue("CONTINUOUS_INTEGRATION") || isEnvTrue("TRAVIS"));
        if (debugMode) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    dumpRecorder();
                }
            });
        }

        debugger = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
    }

    public static boolean isDebug() {
        return debugMode;
    }

    public static boolean isCI() {
        return ci;
    }

    public static boolean isDebugger() {
        return debugger;
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

    public static void exit() {
        exit(0, null, null);
    }

    public static void exit(int code) {
        exit(code, null, null);
    }

    public static void exit(int code, String filename) {
        exit(code, null, filename);
    }

    public static void exit(Throwable t, String filename) {
        exit(1, t, filename);
    }

    public static void exit(Throwable t) {
        exit(1, t, null);
    }

    @SuppressWarnings("CallToThreadDumpStack")
    public static void exit(int code, Throwable t, String filename) {
        final Strand currentStrand = Strand.currentStrand();
        if (flightRecorder != null) {
            flightRecorder.record(1, "DEBUG EXIT REQUEST ON STRAND " + currentStrand + ": " + Arrays.toString(currentStrand.getStackTrace()));
            if (t != null)
                flightRecorder.record(1, "CAUSED BY " + t + ": " + Arrays.toString(currentStrand.getStackTrace()));
            flightRecorder.stop();
        }

        if (requestShutdown.compareAndSet(false, true)) {
            System.err.println("DEBUG EXIT REQUEST ON STRAND " + currentStrand
                    + (currentStrand.isFiber() ? " (THREAD " + Thread.currentThread() + ")" : "")
                    + ": SHUTTING DOWN THE JVM.");
            Thread.dumpStack();
            if (t != null) {
                System.err.println("CAUSED BY " + t);
                t.printStackTrace();
            }
            dumpRecorder(filename);
            if (!isUnitTest()) // Calling System.exit() in gradle unit tests breaks gradle
                System.exit(code);
        }
    }

    public static void record(int level, Object payload) {
        if (!isDebug())
            return;
        if (getGlobalFlightRecorder() == null)
            return;
        getGlobalFlightRecorder().record(level, payload);
    }

    public static void record(int level, Object... payload) {
        if (!isDebug())
            return;
        if (getGlobalFlightRecorder() == null)
            return;
        getGlobalFlightRecorder().record(level, payload);
    }

    public static void dumpRecorder() {
        dumpRecorder(null);
    }

    public static void dumpRecorder(String filename) {
        dumpLock.lock();
        try {
            if (!dumped) {
                if (filename == null) {
                    filename = getDumpFile();
                    if (filename == null || filename.trim().equals(""))
                        filename = null;
                }
                if (filename == null) {
                    System.err.println("NO ERROR LOG FILE SPECIFIED.");
                    return;
                }
                if (flightRecorder != null)
                    flightRecorder.dump(filename);
                dumped = true;
                dumpDone.signalAll();
            } else {
                while (!dumped)
                    dumpDone.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            dumpLock.unlock();
        }

    }

    public static void dumpAfter(final long millis) {
        dumpAfter(millis, FLIGHT_RECORDER_DUMP_FILE);
    }

    public static void dumpAfter(final long millis, final String filename) {
        if (!debugMode)
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

    public static String toString(Object x) {
        if (x == null)
            return "null";
        if (x instanceof boolean[])
            return Arrays.toString((boolean[])x);
        if (x instanceof char[])
            return Arrays.toString((char[])x);
        if (x instanceof byte[])
            return Arrays.toString((byte[])x);
        if (x instanceof short[])
            return Arrays.toString((short[])x);
        if (x instanceof int[])
            return Arrays.toString((int[])x);
        if (x instanceof long[])
            return Arrays.toString((long[])x);
        if (x instanceof float[])
            return Arrays.toString((float[])x);
        if (x instanceof double[])
            return Arrays.toString((double[])x);
        if (x instanceof Object[])
            return Arrays.toString((Object[])x);
        return x.toString();
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

    private static boolean isEnvTrue(String envVar) {
        final String ev = System.getenv(envVar);
        if (ev == null)
            return false;
        try {
            return Boolean.parseBoolean(ev);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getPackageVersion(String packageName) {
        try {
            Package aPackage = Package.getPackage(packageName);
            if (aPackage != null) {
                return aPackage.getImplementationVersion();
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static Path getJarOfClass(String className) {
        return getJarOfClass(findClass(className));
    }

    public static Path getJarOfClass(Class<?> clazz) {
        try {
            if (clazz != null) {
                final URL resource = clazz.getClassLoader().getResource(clazz.getName().replace('.', '/') + ".class");
                if (resource != null) {
                    String p = resource.toString();
                    int idx = p.lastIndexOf('!');
                    if (idx > 0)
                        return Paths.get(new URI(p.substring(0, idx)));
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static String whereIs(String className) {
        return whereIs(findClass(className));
    }

    public static String whereIs(Class<?> clazz) {
        if (clazz == null)
            return null;
        final String resource = clazz.getName().replace('.', '/') + ".class";
        URL url = clazz.getResource(resource);
        if (url == null)
            url = (clazz.getClassLoader() != null ? clazz.getClassLoader() : ClassLoader.getSystemClassLoader()).getResource(resource);
        return url != null ? url.toString() : null;
    }

    private static Class<?> findClass(String className) {
        try {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className, true, Debug.class.getClassLoader());
            } catch (ClassNotFoundException e2) {
                return null;
            }
        }
    }

    public static String getClassLoaderInfo(ClassLoader cl) {
        final StringBuilder sb = new StringBuilder();
        int indent = 0;
        while (cl != null) {
            indent(sb, indent).append(cl.toString()).append('\n');
            if (cl instanceof URLClassLoader)
                indent(sb, indent).append("URLs: ").append(Arrays.toString(((URLClassLoader) cl).getURLs())).append('\n');
            cl = cl.getParent();
            indent += 4;
        }

        return sb.toString();
    }

    public static void printStackTrace(int num, java.io.PrintStream out) {
        StackTraceElement[] trace = new Throwable().getStackTrace();
        if (trace == null)
            out.println("No stack trace");
        else {
            for (int i = 0; i < num && i < trace.length; i++)
                out.println("\tat " + trace[i]);
        }
    }

    public static void printStackTrace(int num, java.io.PrintWriter out) {
        StackTraceElement[] trace = new Throwable().getStackTrace();
        if (trace == null)
            out.println("No stack trace");
        else {
            for (int i = 0; i < num && i < trace.length; i++)
                out.println("\tat " + trace[i]);
        }
    }

    private static StringBuilder indent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++)
            sb.append(' ');
        return sb;
    }

    private Debug() {
    }
}
