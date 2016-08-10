/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.util.*;
import co.paralleluniverse.fibers.*;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableUtils;

import java.lang.reflect.*;
import java.util.Arrays;

/**
 * @author pron
 */
public final class Verify {
    public static final boolean verifyInstrumentation = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation");

    public static <V> void verifyTarget(SuspendableCallable<V> target) {
        Object t = target;
        if (target instanceof SuspendableUtils.VoidSuspendableCallable)
            t = ((SuspendableUtils.VoidSuspendableCallable) target).getRunnable();

        if (t.getClass().getName().contains("$$Lambda$"))
            return;

        if (verifyInstrumentation && !verifyFiberClass(t.getClass()))
            throw new VerifyInstrumentationException("Target class " + t.getClass() + " has not been instrumented.");
    }

    public static boolean verifyFiberClass(Class clazz) {
        boolean res = clazz.isAnnotationPresent(Instrumented.class);
        if (!res)
            res = verifyFiberClass0(clazz); // a second chance
        return res;
    }

    private static boolean verifyFiberClass0(Class clazz) {
        // Sometimes, a child class does not implement any suspendable methods AND is loaded before its superclass (that does). Test for that:
        final Class superclazz = clazz.getSuperclass();
        if (superclazz != null) {
            if (superclazz.isAnnotationPresent(Instrumented.class)) {
                // make sure the child class doesn't have any suspendable methods
                final Method[] ms = clazz.getDeclaredMethods();
                for (final Method m : ms) {
                    for (final Class et : m.getExceptionTypes()) {
                        if (et.equals(SuspendExecution.class))
                            return false;
                    }
                    if (m.isAnnotationPresent(Suspendable.class))
                        return false;
                }
                return true;
            } else
                return verifyFiberClass0(superclazz);
        } else
            return false;
    }

    public static boolean checkInstrumentation() {
        return checkInstrumentation(ExtendedStackTrace.here());
    }

    public static boolean checkInstrumentation(ExtendedStackTrace of) {
        return checkInstrumentation(of, false);
    }

    @SuppressWarnings("null")
    private static boolean checkInstrumentation(ExtendedStackTrace st, boolean fromUncaughtExc) {
        if (fromUncaughtExc && st.get().length > 0 && st.get()[0] != null) {
            final ExtendedStackTraceElement first = st.get()[0];
            if (!first.getDeclaringClass().equals(ClassCastException.class)
                && !(first.getDeclaringClass().equals(NullPointerException.class) &&
                first.getDeclaringClass().getName().startsWith("co.paralleluniverse.fibers")))
                return true;
        }

        boolean ok = true;
        StringBuilder stackTrace = null;

        final ExtendedStackTraceElement[] stes = st.get();
        for (int i = 0; i < stes.length; i++) {
            final ExtendedStackTraceElement ste = stes[i];
            if (ste.getClassName().equals(Thread.class.getName()) && ste.getMethodName().equals("getStackTrace"))
                continue;
            if (ste.getClassName().equals(ExtendedStackTrace.class.getName()))
                continue;
            if (!ok)
                printTraceLine(stackTrace, ste);
            if (ste.getClassName().contains("$$Lambda$"))
                continue;

            if (!ste.getClassName().equals(Fiber.class.getName()) && !ste.getClassName().startsWith(Fiber.class.getName() + '$')
                && !ste.getClassName().equals(Stack.class.getName()) && !SuspendableHelper.isWaiver(ste.getClassName(), ste.getMethodName())) {
                final Class<?> clazz = ste.getDeclaringClass();
                boolean classInstrumented = SuspendableHelper.isInstrumented(clazz);
                final /*Executable*/ Member m = SuspendableHelper.lookupMethod(ste);
                if (m != null) {
                    boolean methodInstrumented = SuspendableHelper.isInstrumented(m);
                    Pair<Boolean, Instrumented> callSiteInstrumented = SuspendableHelper.isCallSiteInstrumented(m, ste.getLineNumber(), ste.getBytecodeIndex(), (i == 0 ? null : stes[i - 1]));
                    if (!classInstrumented || !methodInstrumented || !callSiteInstrumented.getFirst()) {
                        if (ok)
                            stackTrace = initTrace(i, stes);

                        if (!classInstrumented || !methodInstrumented)
                            stackTrace.append(" **");
                        else if (!callSiteInstrumented.getFirst())
                            stackTrace.append(" !! (instrumented suspendable calls at: ")
                                .append(callSitesString(callSiteInstrumented.getSecond()))
                                .append(")");
                        ok = false;
                    }
                } else {
                    if (ok)
                        stackTrace = initTrace(i, stes);

                    stackTrace.append(" **"); // Methods can only be found via source lines in @Instrumented annotations
                    ok = false;
                }
            } else if (ste.getClassName().equals(Fiber.class.getName()) && ste.getMethodName().equals("run1")) {
                if (!ok) {
                    final String str = "Uninstrumented whole methods ('**') or single calls ('!!') detected: " + stackTrace;
                    if (Debug.isUnitTest())
                        throw new VerifyInstrumentationException(str);
                    System.err.println("WARNING: " + str);
                }
                return ok;
            }
        }
        throw new IllegalStateException("Not run through Fiber.exec(). (trace: " + Arrays.toString(stes) + ")");
    }

    private static String callSitesString(Instrumented i) {
        if (i == null)
            return "N/A";

        return
            "BCIs " + Arrays.toString(SuspendableHelper.getPostInstrumentationOffsets(i)) +
                ", lines " + Arrays.toString(SuspendableHelper.getSourceLines(i));
    }

    private static StringBuilder initTrace(int i, ExtendedStackTraceElement[] stes) {
        final StringBuilder stackTrace = new StringBuilder();
        for (int j = 0; j <= i; j++) {
            final ExtendedStackTraceElement ste2 = stes[j];
            if (ste2.getClassName().equals(Thread.class.getName()) && ste2.getMethodName().equals("getStackTrace"))
                continue;
            printTraceLine(stackTrace, ste2);
        }
        return stackTrace;
    }

    private static void printTraceLine(StringBuilder stackTrace, ExtendedStackTraceElement ste) {
        stackTrace.append("\n\tat ").append(ste);
        final Member m = SuspendableHelper.lookupMethod(ste);
        if (SuspendableHelper.isOptimized(m))
            stackTrace.append(" (optimized)");
    }

    private Verify() {}
}
