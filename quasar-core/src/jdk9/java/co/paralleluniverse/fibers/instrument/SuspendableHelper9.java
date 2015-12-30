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

import co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.Stack;
import co.paralleluniverse.strands.SuspendableUtils;

import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * @author circlespainter
 */
public final class SuspendableHelper9 {

    private static Field memberName;
    private static Method getMethodType;

    static {
        try {
            final Class<?> stackFrameInfoClass = Class.forName("java.lang.StackFrameInfo");
            memberName = stackFrameInfoClass.getDeclaredField("memberName");
            memberName.setAccessible(true);
            getMethodType = Class.forName("java.lang.invoke.MemberName").getDeclaredMethod("getMethodType");
            getMethodType.setAccessible(true);
        } catch (final ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static Method lookupMethod(Class<?> declaringClass, String methodName, MethodType t) {
        if (declaringClass == null || methodName == null || t == null)
            return null;

        for (final Method m : declaringClass.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && Arrays.equals(m.getParameterTypes(), t.parameterArray())
                && t.returnType().equals(m.getReturnType())) {
                return m;
            }
        }

        return null;
    }

    public static Method lookupMethod(StackWalker.StackFrame sf) {
        try {
            return lookupMethod(sf.getDeclaringClass(), sf.getMethodName(), (MethodType) getMethodType.invoke(memberName.get(sf)));
        } catch (final InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isUpperFiberRuntime(String className) {
        return
            Fiber.class.getName().equals(className) ||
            Stack.class.getName().equals(className) ||
            LiveInstrumentation.class.getName().equals(className) ||
            Verify.class.getName().equals(className) ||
            ForkJoinWorkerThread.class.getName().equals(className) ||
            ForkJoinTask.class.getName().equals(className) ||
            ParkableForkJoinTask.class.getName().equals(className) ||
            className.startsWith(SuspendableUtils.VoidSuspendableCallable.class.getName()) ||
            className.startsWith(ForkJoinPool.class.getName()) ||
            className.startsWith(FiberForkJoinScheduler.class.getName());
    }

    private SuspendableHelper9() {}

    public static boolean isFiber(String cn) {
        return "co.paralleluniverse.fibers.Fiber".equals(cn);
    }
}
