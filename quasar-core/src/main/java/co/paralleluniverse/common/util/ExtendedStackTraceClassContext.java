/*
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
package co.paralleluniverse.common.util;

import java.security.PrivilegedAction;
import static java.security.AccessController.doPrivileged;

/**
 * @author pron
 */
class ExtendedStackTraceClassContext extends ExtendedStackTrace {
    private static final ClassContext classContextGenerator = doPrivileged(new CreateClassContext());
    private ExtendedStackTraceElement[] est;
    private final Class<?>[] classContext;

    ExtendedStackTraceClassContext() {
        super(new Throwable());
        this.classContext = classContextGenerator.getClassContext();

//        int i = 0;
//        for (Class c : classContext)
//            System.out.println("== " + i++ + " " + c.getName());
//        System.out.println("");
//        i = 0;
//        for (StackTraceElement e : t.getStackTrace())
//            System.out.println("-- " + i++ + " " + e);
    }

    @Override
    public synchronized ExtendedStackTraceElement[] get() {
        if (est == null) {
            final StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                est = new ExtendedStackTraceElement[st.length - 1];
                for (int i = 1, k = 2; i < st.length; i++, k++) {
                    if (skipCTX(classContext[k])) {
                        i--;
                    } else {
                        final StackTraceElement ste = st[i];
                        final Class<?> clazz;

                        if (skipSTE(st[i])) {
                            k--;
                            clazz = null;
                        } else {
                            clazz = classContext[k];
                        }

                        est[i - 1] = new BasicExtendedStackTraceElement(ste, clazz);
                        // System.out.println(">>>> " + k + ": " + (clazz != null ? clazz.getName() : null) + " :: " + i + ": " + ste);
                    }
                }
            }
        }
        return est;
    }

    static boolean skipSTE(StackTraceElement ste) {
        final String className = ste.getClassName();
        return (className.startsWith("sun.reflect")
                || className.startsWith("jdk.internal.reflect.")
                || className.equals("java.lang.reflect.Method")
                || className.startsWith("java.lang.invoke.")
                // Originated from http://bugs.java.com/view_bug.do?bug_id=8025636, Quasar PR #207
                || className.contains("$$Lambda$"));
    }

    private static boolean skipCTX(Class<?> c) {
        final String className = c.getName();
        return (className.startsWith("java.lang.invoke.")
                // Originated from http://bugs.java.com/view_bug.do?bug_id=8025636, Quasar PR #207
                || className.contains("$$Lambda$"));
    }

    private static class ClassContext extends SecurityManager {
        @Override
        public Class[] getClassContext() {
            return super.getClassContext();
        }
    }

    private static final class CreateClassContext implements PrivilegedAction<ClassContext> {
        @Override
        public ClassContext run() {
            return new ClassContext();
        }
    }
}
