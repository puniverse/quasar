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
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.asm.ASMUtil;
import co.paralleluniverse.common.util.ExtendedStackTraceElement;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.fibers.suspend.Instrumented;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

import static co.paralleluniverse.fibers.instrument.SuspendableHelper.isSyntheticAndNotLambda;
import static co.paralleluniverse.fibers.instrument.SuspendableHelper.isWaiver;
import static java.security.AccessController.doPrivileged;

/**
 *
 * @author pron
 */
final class FiberHelper {
    public static /*Executable*/ Member lookupMethod(ExtendedStackTraceElement ste) {
        if (ste.getDeclaringClass() == null)
            return null;

        if (ste.getMethod() != null)
            return ste.getMethod();

        return doPrivileged(new LookupMethod(ste));
    }

    static final class LookupMethod implements PrivilegedAction<Method> {
        private final ExtendedStackTraceElement ste;

        LookupMethod(ExtendedStackTraceElement ste) {
            this.ste = ste;
        }

        @Override
        public Method run() {
            for (final Method m : ste.getDeclaringClass().getDeclaredMethods()) {
                if (m.getName().equals(ste.getMethodName())) {
                    final Instrumented i = getAnnotation(m, Instrumented.class);
                    if (m.isSynthetic() || isWaiver(m.getDeclaringClass().getName(), m.getName()) || i != null && ste.getLineNumber() >= i.methodStart() && ste.getLineNumber() <= i.methodEnd())
                        return m;
                }
            }
            return null;
        }
    }

    public static Pair<Boolean, Instrumented> isCallSiteInstrumented(/*Executable*/ Member m, int sourceLine, int bci, ExtendedStackTraceElement[] stes, int currentSteIdx) {
        if (m == null)
            return new Pair<>(false, null);

        if (isSyntheticAndNotLambda(m))
            return new Pair<>(true, null);

        final ExtendedStackTraceElement calleeSte = currentSteIdx - 1 >= 0 ? stes[currentSteIdx - 1] : null;

        if (calleeSte != null
                // `verifySuspend` and `popMethod` calls are not suspendable call sites, not verifying them.
                && ((calleeSte.getClassName().equals(Fiber.class.getName()) && calleeSte.getMethodName().equals("verifySuspend"))
                || (calleeSte.getClassName().equals(Stack.class.getName()) && calleeSte.getMethodName().equals("popMethod")))) {
            return new Pair<>(true, null);
        } else {
            final Instrumented i = getAnnotation(m, Instrumented.class);
            if (i == null) {
                return new Pair<>(false, null);
            }

            if (calleeSte != null && i.suspendableCallSiteNames() != null) { // check by callsite name (fails for bootstrapped lambdas)
                final Member callee = calleeSte.getMethod();
                if (callee == null) {
                    final String methodName = "." + calleeSte.getMethodName() + "(";
                    for (String callsite : i.suspendableCallSiteNames()) {
                        if (callsite.contains(methodName)) {
                            return new Pair<>(true, i);
                        }
                    }
                } else {
                    final String nameAndDescSuffix = "." + callee.getName() + ASMUtil.getDescriptor(callee);
                    final String[] callsites = i.suspendableCallSiteNames();
                    for (String callsite : callsites) {
                        if (callsite.endsWith(nameAndDescSuffix)) {
                            final String ownerName = getCallsiteOwner(callsite);
                            Class<?> callsiteOwner;
                            try {
                                callsiteOwner = Class.forName(ownerName, true, Thread.currentThread().getContextClassLoader());
                            } catch (ClassNotFoundException e) {
                                try {
                                    callsiteOwner = Class.forName(ownerName, true, FiberHelper.class.getClassLoader());
                                } catch (ClassNotFoundException e2) {
                                    callsiteOwner = null;
                                }
                            }
                            if (callsiteOwner != null) {
                                final Class<?> owner = callee.getDeclaringClass();
                                if (doPrivileged(new DeclareInCommonAncestor(nameAndDescSuffix, owner, callsiteOwner))) {
                                    return new Pair<>(true, i);
                                }
                            }
                        }
                    }
                }
            }
            if (bci >= 0) { // check by bci; may be brittle
                final int[] scs = i.suspendableCallSitesOffsetsAfterInstr();
                for (int j : scs) {
                    if (j == bci)
                        return new Pair<>(true, i);
                }
            }
            else if (sourceLine >= 0) { // check by source line
                final int[] scs = i.suspendableCallSites();
                for (int j : scs) {
                    if (j == sourceLine)
                        return new Pair<>(true, i);
                }
            }
            return new Pair<>(false, i);
        }
    }

    static final class DeclareInCommonAncestor implements PrivilegedAction<Boolean> {
        private final String nameAndDescSuffix;
        private final Class<?> owner;
        private final Class<?> callsiteOwner;

        DeclareInCommonAncestor(String nameAndDescSuffix, Class<?> owner, Class<?> callsiteOwner) {
            this.nameAndDescSuffix = nameAndDescSuffix;
            this.owner = owner;
            this.callsiteOwner = callsiteOwner;
        }

        @Override
        public Boolean run() {
            return declareInCommonAncestor(nameAndDescSuffix, owner, callsiteOwner);
        }
    }

    private static boolean declareInCommonAncestor(String nameAndDescSuffix, Class<?> c1, Class<?> c2) {
        if (nameAndDescSuffix == null || c1 == null || c2 == null)
            return false;

        if (c1.isAssignableFrom(c2))
            return hasMethodWithDescriptor(nameAndDescSuffix, c1);

        if (c2.isAssignableFrom(c1))
            return hasMethodWithDescriptor(nameAndDescSuffix, c2);

        return declareInCommonAncestor(nameAndDescSuffix, c1.getSuperclass(), c2) ||
            declareInCommonAncestor(nameAndDescSuffix, c1.getInterfaces(), c2);
    }

    private static boolean hasMethodWithDescriptor(String nameAndDescSuffix, Class<?> c) {
        if (nameAndDescSuffix == null || c == null)
            return false;

        for (final Method m : c.getDeclaredMethods()) {
            final String n = "." + m.getName() + ASMUtil.getDescriptor(m);
            if (nameAndDescSuffix.equals(n))
                return true;
        }

        if (hasMethodWithDescriptor(nameAndDescSuffix, c.getSuperclass()))
            return true;

        for (final Class<?> i : c.getInterfaces()) {
            if (hasMethodWithDescriptor(nameAndDescSuffix, i))
                return true;
        }

        return false;
    }

    private static boolean declareInCommonAncestor(String nameAndDescSuffix, Class<?>[] c1s, Class<?> c2) {
        if (nameAndDescSuffix == null || c1s == null || c2 == null)
            return false;

        for (final Class<?> c1 : c1s) {
            if (declareInCommonAncestor(nameAndDescSuffix, c1, c2))
                return true;
        }

        return false;
    }

    static String getCallsiteOwner(String callsiteName) {
        return callsiteName.substring(0, callsiteName.indexOf('.')).replace('/', '.');
    }

    static String getCallsiteName(String callsiteName) {
        return callsiteName.substring(callsiteName.indexOf('.') + 1, callsiteName.indexOf('('));
    }

    static String getCallsiteDesc(String callsiteName) {
        return callsiteName.substring(callsiteName.indexOf('('));
    }

    static boolean isInstrumented(Member m) {
        return m != null && (isSyntheticAndNotLambda(m) || getAnnotation(m, Instrumented.class) != null);
    }

    static boolean isOptimized(Member m) {
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

    private FiberHelper() {
    }
}
