/*
 * Quasar: lightweight threads and actors for the JVM.
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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.Instrumented;
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

    public static boolean isInstrumented(Class clazz, String methodName) {
        if (clazz == null)
            return false;

        if (isInstrumented(clazz.getMethods(), methodName))
            return true;
        if (isInstrumented(clazz.getDeclaredMethods(), methodName))
            return true;
        return isInstrumented(clazz.getSuperclass(), methodName);
    }

    private static boolean isInstrumented(Method[] methods, String methodName) {
        for (Method m : methods) {
            if (methodName.equals(m.getName()) && isInstrumented(m))
                return true;
        }
        return false;
    }

    public static boolean isInstrumented(Method method) {
        return method.getAnnotation(Instrumented.class) != null;
    }

    public static void addWaiver(String className, String methodName) {
        waivers.add(new Pair<String, String>(className, methodName));
    }

    public static boolean isWaiver(String className, String methodName) {
        if (className.startsWith("java.lang.reflect")
                || className.startsWith("sun.reflect")
                || className.startsWith("com.sun.proxy")
                || (className.equals("co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable") && methodName.equals("run")))
            return true;
        return waivers.contains(new Pair<String, String>(className, methodName));
    }

    private SuspendableHelper() {
    }
}
