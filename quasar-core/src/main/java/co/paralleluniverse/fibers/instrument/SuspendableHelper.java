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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.util.ExtendedStackTraceElement;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Instrumented;
import co.paralleluniverse.fibers.Stack;
import co.paralleluniverse.fibers.SuspendableCallSite;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
// import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

/**
 * @author pron
 */
public final class SuspendableHelper {
    static boolean javaAgent;
    static final Set<Pair<String, String>> waivers = Collections.newSetFromMap(MapUtil.<Pair<String, String>, Boolean>newConcurrentHashMap());

    public static boolean isJavaAgentActive() {
        return javaAgent;
    }

    public static boolean isInstrumented(Class clazz) {
        return clazz != null && clazz.isAnnotationPresent(Instrumented.class);
    }

    public static /*Executable*/ Member lookupMethod(ExtendedStackTraceElement ste) {
        if (ste.getDeclaringClass() == null)
            return null;

        if (ste.getMethod() != null)
            return ste.getMethod();

        for (Method m : ste.getDeclaringClass().getDeclaredMethods()) {
            if (m.getName().equals(ste.getMethodName())) {
                final Instrumented i = getAnnotation(m, Instrumented.class);
                if (m.isSynthetic() || isWaiver(m.getDeclaringClass().getName(), m.getName()) || i != null && ste.getLineNumber() >= i.methodStartSourceLine() && ste.getLineNumber() <= i.methodEndSourceLine())
                    return m;
            }
        }
        return null;
    }

    public static Pair<Boolean, int[]> isCallSiteInstrumented(/*Executable*/ Member m, int sourceLine, ExtendedStackTraceElement optUpperSte) {
        if (m == null)
            return new Pair<>(false, null);

        if (isSyntheticAndNotLambda(m))
            return new Pair<>(true, null);

        if (optUpperSte != null
                // `verifySuspend` and `popMethod` calls are not suspendable call sites, not verifying them.
                && ((optUpperSte.getClassName().equals(Fiber.class.getName()) && optUpperSte.getMethodName().equals("verifySuspend"))
                || (optUpperSte.getClassName().equals(Stack.class.getName()) && optUpperSte.getMethodName().equals("popMethod")))) {
            return new Pair<>(true, null);
        } else {
            final Instrumented i = getAnnotation(m, Instrumented.class);
            if (i != null) {
                final int[] sourceLines = getSourceLines(i);
                for (int j : sourceLines) {
                    if (j == sourceLine)
                        return new Pair<>(true, sourceLines);
                }
            }
        }

        return new Pair<>(false, null);
    }

    static int[] getSourceLines(Instrumented a) {
        final SuspendableCallSite[] susCallSites = a.methodSuspendableCallSites();
        final int[] ret = new int[susCallSites.length];
        for (int i = 0 ; i < ret.length ; i++)
            ret[i] = susCallSites[i].sourceLine();
        return ret;
    }

    static int[] getPostInstrumentationOffsets(Instrumented a) {
        final SuspendableCallSite[] susCallSites = a.methodSuspendableCallSites();
        final int[] ret = new int[susCallSites.length];
        for (int i = 0 ; i < ret.length ; i++)
            ret[i] = susCallSites[i].postInstrumentationOffset();
        return ret;
    }

    public static boolean isInstrumented(Member m) {
        return m != null && (isSyntheticAndNotLambda(m) || getAnnotation(m, Instrumented.class) != null);
    }
    
    public static boolean isSyntheticAndNotLambda(Member m) {
        return m.isSynthetic() && !m.getName().startsWith("lambda$");
    }
    
    public static boolean isOptimized(Member m) {
        if (m == null)
            return false;

        final Instrumented i = getAnnotation(m, Instrumented.class);
        return (i != null && i.isMethodInstrumentationOptimized());
    }

    public static <T extends Annotation> T getAnnotation(Member m, Class<T> annotationClass) {
        if (m == null || annotationClass == null)
            return  null;

        if (m instanceof Constructor)
            return ((Constructor<?>)m).getAnnotation(annotationClass);
        else
            return ((Method)m).getAnnotation(annotationClass);
    }

    public static void addWaiver(String className, String methodName) {
        waivers.add(new Pair<>(className, methodName));
    }

    public static boolean isWaiver(String className, String methodName) {
        return
            className.startsWith("java.lang.reflect") ||
            className.startsWith("sun.reflect") ||
            className.startsWith("com.sun.proxy") ||
            className.contains("$ByteBuddy$") ||
            (className.equals("co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable") &&
                methodName.equals("run")) ||
            waivers.contains(new Pair<>(className, methodName));
    }

    private SuspendableHelper() {}
}
