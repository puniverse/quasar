/*
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
package co.paralleluniverse.common.util;

/**
 * @author pron
 */
class ExtendedStackTraceClassContext extends ExtendedStackTrace {
    private static final ClassContext classContextGenerator = new ClassContext();
    private ExtendedStackTraceElement[] est;
    private final Class[] classContext;

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
        return (ste.getClassName().startsWith("sun.reflect")
                || ste.getClassName().equals("java.lang.reflect.Method")
                || ste.getClassName().startsWith("java.lang.invoke."));
    }

    private static boolean skipCTX(Class c) {
        return (c.getName().startsWith("java.lang.invoke.")
                // candrews PR#207: next one needed since after 8u60, see http://bugs.java.com/view_bug.do?bug_id=802563;
                //                  reported @ http://bugreport.java.com/, Review ID: JI-9040355
                /*|| c.getName().contains("$$Lambda$")*/); // Commenting out for tests on pre-8u60
    }

    private static class ClassContext extends SecurityManager {
        @Override
        public Class[] getClassContext() {
            return super.getClassContext();
        }
    }
}
