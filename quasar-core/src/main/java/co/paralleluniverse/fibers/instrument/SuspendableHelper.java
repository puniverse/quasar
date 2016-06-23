/*
 * Quasar: lightweight threads and actors for the JVM.
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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.util.ExtendedStackTraceElement;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Instrumented;
import co.paralleluniverse.fibers.Stack;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
// import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

/**
 *
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

        for (final Method m : ste.getDeclaringClass().getDeclaredMethods()) {
            if (m.getName().equals(ste.getMethodName())) {
                final Instrumented i = getAnnotation(m, Instrumented.class);
                if (m.isSynthetic() || isWaiver(m.getDeclaringClass().getName(), m.getName()) || i != null && ste.getLineNumber() >= i.methodStart() && ste.getLineNumber() <= i.methodEnd())
                    return m;
            }
        }
        return null;
    }

    public static Pair<Boolean, Instrumented> isCallSiteInstrumented(/*Executable*/ Member m, int sourceLine, int bci, ExtendedStackTraceElement[] stes, int currentSteIdx) {
        if (m == null)
            return new Pair<>(false, null);

        if (isSyntheticAndNotLambda(m))
            return new Pair<>(true, null);

        if (currentSteIdx - 1 >= 0
                // `verifySuspend` and `popMethod` calls are not suspendable call sites, not verifying them.
                && ((stes[currentSteIdx - 1].getClassName().equals(Fiber.class.getName()) && stes[currentSteIdx - 1].getMethodName().equals("verifySuspend"))
                || (stes[currentSteIdx - 1].getClassName().equals(Stack.class.getName()) && stes[currentSteIdx - 1].getMethodName().equals("popMethod")))) {
            return new Pair<>(true, null);
        } else {
            final Instrumented i = getAnnotation(m, Instrumented.class);
            if (i != null) {
                if (bci >= 0) { // Prefer BCI matching as it's unambiguous
                    final int[] scs = i.suspendableCallSitesOffsetsAfterInstr();
                    for (int j : scs) {
                        if (j == bci)
                            return new Pair<>(true, i);
                    }
                } else if (sourceLine >= 0){
                    final int[] scs = i.suspendableCallSites();
                    for (int j : scs) {
                        if (j == sourceLine)
                            return new Pair<>(true, i);
                    }
                }

                return new Pair<>(false, i);
            }

            return new Pair<>(false, null);
        }
    }

    public static boolean isInstrumented(Member m) {
        return m != null && (isSyntheticAndNotLambda(m) || getAnnotation(m, Instrumented.class) != null);
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean isSyntheticAndNotLambda(Member m) {
        return m.isSynthetic() && !m.getName().startsWith(Classes.LAMBDA_METHOD_PREFIX);
    }

    public static boolean isOptimized(Member m) {
        if (m == null)
            return false;

        final Instrumented i = getAnnotation(m, Instrumented.class);
        return (i != null && i.methodOptimized());
    }

    private static <T extends Annotation> T getAnnotation(Member m, Class<T> annotationClass) {
        if (m == null || annotationClass == null)
            return  null;

        if (m instanceof Constructor)
            return ((Constructor<?>)m).getAnnotation(annotationClass);
        else
            return ((Method)m).getAnnotation(annotationClass);
    }

    @SuppressWarnings("WeakerAccess")
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
            (className.equals("co.paralleluniverse.strands.dataflow.Var") &&
                methodName.equals("set")) ||
            waivers.contains(new Pair<>(className, methodName));
    }

    private SuspendableHelper() {
    }
}
