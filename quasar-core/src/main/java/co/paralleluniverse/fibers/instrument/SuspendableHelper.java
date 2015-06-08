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
        // Easy, we're given the class.
        return clazz.isAnnotationPresent(Instrumented.class);
    }

    public static boolean isInstrumented(Class clazz, String methodName) {
        if (clazz == null)
            return false;

        // The method name alone doesn't identify a method in the class
        // (current assumption here).
        //
        // We could also have the caller's line number and all the
        // invoked signatures there (by inspecting bytecode or tailor-made
        // annotations built during instrumentation) but that could be
        // insufficient too: think of different overloads called in the
        // same line).
        //
        // The best we can do in this latter case is consider them all
        // and warn the user that we could be issuing a false alarm.

        // TODO Fix method identification.
        // TODO Fix "any match ok" logic.

        if (isInstrumented(clazz.getMethods(), methodName))
            return true;
        if (isInstrumented(clazz.getDeclaredMethods(), methodName))
            return true;
        return isInstrumented(clazz.getSuperclass(), methodName);
    }


    private static boolean isInstrumented(Method[] methods, String methodName) {
        // TODO Fix method identification.
        // TODO Then fix "any match ok" logic.

        for (Method m : methods) {
            if (methodName.equals(m.getName()) && isInstrumented(m))
                return true;
        }
        return false;
    }

    public static Pair<Boolean, int[]> isCallSiteInstrumented(StackTraceElement[] stes, int currentSteIdx, Class clazz, String methodName, int lineNumber) {
        if (clazz == null)
            return new Pair(false, null);

        StackTraceElement ste = stes[currentSteIdx];
        if (currentSteIdx - 1 >= 0
            &&
            ((stes[currentSteIdx - 1].getClassName().equals(Fiber.class.getName()) && stes[currentSteIdx - 1].getMethodName().equals("verifySuspend"))
             || (stes[currentSteIdx - 1].getClassName().equals(Stack.class.getName()) && stes[currentSteIdx - 1].getMethodName().equals("popMethod")))) {
            //  (f.e.) FiberAsync calls directly "verifySuspend" and calls to "popMethod" (which calls verifySuspend) are checking correctly only call sites
            // below this frame (because the "popMethod" call at the exit of an instrumented method is not a suspendable call site).
            return new Pair(true, null);
        }
            
        // TODO Fix method identification.
        // TODO Then fix "any match ok" logic.

        Pair<Boolean, int[]> methodsRes = isCallSiteInstrumented(clazz.getMethods(), methodName, lineNumber);
        if (methodsRes.getFirst())
            return methodsRes;
        Pair<Boolean, int[]> declaredMethodsRes = isCallSiteInstrumented(clazz.getDeclaredMethods(), methodName, lineNumber);
        if (declaredMethodsRes.getFirst())
            return declaredMethodsRes;
        return isCallSiteInstrumented(stes, currentSteIdx, clazz.getSuperclass(), methodName, lineNumber);
    }
    
    private static Pair<Boolean, int[]>  isCallSiteInstrumented(Method[] methods, String methodName, int lineNumber) {
        // TODO Fix method identification.
        // TODO Then fix "any match ok" logic.

        for (Method m : methods) {
            if (methodName.equals(m.getName())) {
                Instrumented i = m.getAnnotation(Instrumented.class);
                if (i != null) {
                    for(int j : i.suspendableCallsites()) {
                        if (j == lineNumber)
                            return new Pair<>(true, i.suspendableCallsites());
                    }
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
