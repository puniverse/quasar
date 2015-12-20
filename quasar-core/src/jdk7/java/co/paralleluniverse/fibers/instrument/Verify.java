/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.ExtendedStackTrace;
import co.paralleluniverse.common.util.ExtendedStackTraceElement;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Stack;
import co.paralleluniverse.fibers.VerifyInstrumentationException;

import java.lang.reflect.Member;
import java.util.Arrays;

/**
 * @author pron
 */
public final class Verify {

    public static boolean checkInstrumentation() {
        return checkInstrumentation(ExtendedStackTrace.here());
    }

    @SuppressWarnings("null")
    public static boolean checkInstrumentation(ExtendedStackTrace st) {
        final boolean[] ok_last = new boolean[] { true, false };
        final StringBuilder stackTrace = new StringBuilder();

        final ExtendedStackTraceElement[] stes = st.get();
        for (int i = 0; i < stes.length; i++) {
            final ExtendedStackTraceElement ste = stes[i];
            checkInstrumentation(ok_last, ste, (i == 0 ? null : stes[i - 1]), stackTrace, stes, i);
            if (ok_last[1]) {
                if (!ok_last[0]) {
                    final String str = "Uninstrumented methods (marked '**') or call-sites (marked '!!') detected on the call stack: " + stackTrace;
                    if (Debug.isUnitTest())
                        throw new VerifyInstrumentationException(str);
                    System.err.println("WARNING: " + str);
                }

                return ok_last[0];
            }
        }
        throw new IllegalStateException("Not run through Fiber.exec(). (trace: " + Arrays.toString(stes) + ")");
    }

    private static void checkInstrumentation(boolean[] ok_last, ExtendedStackTraceElement ste, ExtendedStackTraceElement optUpperSte,
                                             StringBuilder optStackTrace, ExtendedStackTraceElement[] optStes, Integer optCurrStesIdx) {
        if (ste.getClassName().equals(Thread.class.getName()) && ste.getMethodName().equals("getStackTrace"))
            return;
        if (ste.getClassName().equals(ExtendedStackTrace.class.getName()))
            return;
        if (optStackTrace != null && !ok_last[0])
            printTraceLine(optStackTrace, ste);
        if (ste.getClassName().contains("$$Lambda$"))
            return;

        if (!ste.getClassName().equals(Fiber.class.getName()) && !ste.getClassName().startsWith(Fiber.class.getName() + '$')
            && !ste.getClassName().equals(Stack.class.getName()) && !SuspendableHelper.isWaiver(ste.getClassName(), ste.getMethodName())) {
            final Class<?> clazz = ste.getDeclaringClass();
            boolean classInstrumented = SuspendableHelper.isInstrumented(clazz);
            final /*Executable*/ Member m = SuspendableHelper.lookupMethod(ste);
            if (m != null) {
                boolean methodInstrumented = SuspendableHelper.isInstrumented(m);
                Pair<Boolean, int[]> callSiteInstrumented = SuspendableHelper.isCallSiteInstrumented(m, ste.getLineNumber(), optUpperSte);
                if (!classInstrumented || !methodInstrumented || !callSiteInstrumented.getFirst()) {
                    if (ok_last[0] && optStackTrace != null && optStes != null && optCurrStesIdx != null)
                        initTrace(optStackTrace, optStes, optCurrStesIdx);

                    if (optStackTrace != null) {
                        if (!classInstrumented || !methodInstrumented)
                            optStackTrace.append(" **");
                        else if (!callSiteInstrumented.getFirst())
                            optStackTrace.append(" !! (instrumented suspendable calls at: ")
                                .append(callSiteInstrumented.getSecond() == null ? "[]" : Arrays.toString(callSiteInstrumented.getSecond()))
                                .append(")");
                    }
                    ok_last[0] = false;
                }
            } else {
                if (optStackTrace != null && optStes != null && optCurrStesIdx != null) {
                    if (ok_last[0])
                        initTrace(optStackTrace, optStes, optCurrStesIdx);
                    optStackTrace.append(" **"); // Methods can only be found via source lines in @Instrumented annotations
                }

                ok_last[0] = false;
            }
        } else if (ste.getClassName().equals(Fiber.class.getName()) && ste.getMethodName().equals("run1")) {
            ok_last[1] = true;
        }
    }

    private static void initTrace(StringBuilder stackTrace, ExtendedStackTraceElement[] stes, int i) {
        for (int j = 0; j <= i; j++) {
            final ExtendedStackTraceElement ste2 = stes[j];
            if (ste2.getClassName().equals(Thread.class.getName()) && ste2.getMethodName().equals("getStackTrace"))
                continue;
            printTraceLine(stackTrace, ste2);
        }
    }

    private static void printTraceLine(StringBuilder stackTrace, ExtendedStackTraceElement ste) {
        stackTrace.append("\n\tat ").append(ste);
        final Member m = SuspendableHelper.lookupMethod(ste);
        if (SuspendableHelper.isOptimized(m))
            stackTrace.append(" (optimized)");
    }

    private Verify() {}
}
