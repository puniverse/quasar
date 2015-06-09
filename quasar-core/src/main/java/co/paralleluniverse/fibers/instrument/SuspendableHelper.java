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

import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Instrumented;
import co.paralleluniverse.fibers.Stack;
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
        return clazz.isAnnotationPresent(Instrumented.class);
    }

    public static Method lookupMethod(Class clazz, String methodName, int sourceLine) {
        if (clazz == null || methodName == null)
            return null;

        for(Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                Instrumented i = m.getAnnotation(Instrumented.class);
                if (isWaiver(m.getDeclaringClass().getName(), m.getName()) || i != null && sourceLine >= i.methodStart() && sourceLine <= i.methodEnd())
                    return m;
            }
        }
        return null;
    }

    public static Pair<Boolean, int[]> isCallSiteInstrumented(Method m, int sourceLine, StackTraceElement[] stes, int currentSteIdx) {
        if (m == null)
            return new Pair<>(false, null);

        StackTraceElement ste = stes[currentSteIdx];
        if (currentSteIdx - 1 >= 0
            &&
            ((stes[currentSteIdx - 1].getClassName().equals(Fiber.class.getName()) && stes[currentSteIdx - 1].getMethodName().equals("verifySuspend"))
             || (stes[currentSteIdx - 1].getClassName().equals(Stack.class.getName()) && stes[currentSteIdx - 1].getMethodName().equals("popMethod")))) {
            //  "verifySuspend" and "popMethod" calls are not suspendable call sites, not verifying them.
            return new Pair<>(true, null);
        } else {
            Instrumented i = m.getAnnotation(Instrumented.class);
            if (i != null) {
                for(int j : i.suspendableCallsites()) {
                    if (j == sourceLine)
                        return new Pair<>(true, i.suspendableCallsites());
                }
            }
        }
            
        return new Pair<>(false, null);
    }
    
    public static boolean isInstrumented(Method method) {
        return method.getAnnotation(Instrumented.class) != null;
    }

    public static void addWaiver(String className, String methodName) {
        waivers.add(new Pair<>(className, methodName));
    }

    public static boolean isWaiver(String className, String methodName) {
        if (className.startsWith("java.lang.reflect")
                || className.startsWith("sun.reflect")
                || className.startsWith("com.sun.proxy")
                || (className.equals("co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable") && methodName.equals("run")))
            return true;
        return waivers.contains(new Pair<>(className, methodName));
    }

    private SuspendableHelper() {
    }
}
