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

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import static co.paralleluniverse.common.asm.ASMUtil.findMethod;
import static co.paralleluniverse.common.asm.ASMUtil.getDescriptor;
import static java.security.AccessController.doPrivileged;

/**
 * @author pron
 */
public class ExtendedStackTrace implements Iterable<ExtendedStackTraceElement> {
    public static ExtendedStackTrace of(Throwable t) {
        try {
            return new ExtendedStackTraceHotSpot(t);
        } catch (Throwable e) {
            return new ExtendedStackTrace(t);
        }
    }

    public static ExtendedStackTrace here() {
        try {
            return new ExtendedStackTraceHotSpot(new Throwable());
        } catch (Throwable e) {
            return new ExtendedStackTraceClassContext();
        }
    }

    protected final Throwable t;
    private ExtendedStackTraceElement[] est;
//    private transient Map<Class<?>, Member[]> methods; // cache

    protected ExtendedStackTrace(Throwable t) {
        this.t = t;
    }

    @Override
    public Iterator<ExtendedStackTraceElement> iterator() {
        return Arrays.asList(get()).iterator();
    }

    public ExtendedStackTraceElement[] get() {
        synchronized (this) {
            if (est == null) {
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    est = new ExtendedStackTraceElement[st.length];
                    for (int i = 0; i < st.length; i++)
                        est[i] = new BasicExtendedStackTraceElement(st[i]);
                }
            }
            return est;
        }
    }

    protected /*Executable*/ Member getMethod(final ExtendedStackTraceElement este) {
        if (este.getDeclaringClass() == null)
            return null;
        Member[] ms = getMethods(este.getDeclaringClass());
        Member method = null;

        for (Member m : ms) {
            if (este.getMethodName().equals(m.getName())) {
                if (method == null)
                    method = m;
                else {
                    method = null; // more than one match
                    break;
                }
            }
        }
        if (method == null && este.getLineNumber() >= 0) {
            try {
                final AtomicReference<String> descriptor = new AtomicReference<>();
                findMethod(este.getDeclaringClass(), este.getMethodName(), este.getLineNumber(), descriptor);

                if (descriptor.get() != null) {
                    final String desc = descriptor.get();
                    for (Member m : ms) {
                        if (este.getMethodName().equals(getName(m)) && desc.equals(getDescriptor(m))) {
                            method = m;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                if (!(e instanceof UnsupportedOperationException))
                    e.printStackTrace();
            }
        }

        return method;
    }

    protected static String getName(Member m) {
        if (m instanceof Constructor)
            return "<init>";
        return ((Method)m).getName();
    }

    protected final Member[] getMethods(Class<?> clazz) {
        return doPrivileged(new GetMethods(clazz));
    }

    private static final class GetMethods implements PrivilegedAction<Member[]> {
        private final Class<?> clazz;

        GetMethods(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Member[] run() {
            Method[] ms = clazz.getDeclaredMethods();
            Constructor<?>[] cs = clazz.getDeclaredConstructors();
            Member[] es = new Member[ms.length + cs.length];
            System.arraycopy(cs, 0, es, 0, cs.length);
            System.arraycopy(ms, 0, es, cs.length, ms.length);
            return es;
        }
    }

    protected class BasicExtendedStackTraceElement extends ExtendedStackTraceElement {
        protected BasicExtendedStackTraceElement(StackTraceElement ste, Class<?> clazz, Method method, int bci) {
            super(ste, clazz, method, bci);
        }

        protected BasicExtendedStackTraceElement(StackTraceElement ste, Class<?> clazz) {
            super(ste, clazz, null, -1);
        }

        protected BasicExtendedStackTraceElement(StackTraceElement ste) {
            super(ste, null, null, -1);
        }

        @Override
        public Member getMethod() {
            if (method == null) {
                method = ExtendedStackTrace.this.getMethod(this);
                if (method != null && !getMethodName().equals(getName(method))) {
                    throw new IllegalStateException("Method name mismatch: " + getMethodName() + ", " + method.getName());
                    // method = null;
                }
            }
            return method;
        }

        @Override
        public Class<?> getDeclaringClass() {
            if (clazz == null) {
                try {
                    clazz = Class.forName(getClassName(), true, Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    try {
                        clazz = Class.forName(getClassName(), true, getClass().getClassLoader());
                    } catch (ClassNotFoundException e2) {
                    }
                }
            }
            return clazz;
        }
    }
}
