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

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static co.paralleluniverse.fibers.instrument.Classes.LAMBDA_METHOD_PREFIX;

/**
 *
 * @author pron
 */
public final class SuspendableHelper {
    static boolean javaAgent;

    private static final Set<Pair<String, String>> waivers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static boolean isJavaAgentActive() {
        return javaAgent;
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean isSyntheticAndNotLambda(Member m) {
        return m.isSynthetic() && !m.getName().startsWith(LAMBDA_METHOD_PREFIX);
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
