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

import co.paralleluniverse.common.reflection.GetAccessDeclaredField;
import co.paralleluniverse.common.reflection.GetAccessDeclaredMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.Iterator;

import static co.paralleluniverse.common.util.Exceptions.rethrow;
import static java.security.AccessController.doPrivileged;

/**
 * This classes uses internal HotSpot data to retrieve a more detailed stacktrace from a {@link Throwable}.
 * 
 * Works only on HotSpot, versions 8 and 9.
 * 
 * @author pron
 */
class ExtendedStackTraceHotSpot extends ExtendedStackTrace {
    /*
     * hotspot/src/share/vm/classfile/javaClasses.hpp
     * hotspot/src/share/vm/classfile/javaClasses.cpp
     */
    private ExtendedStackTraceElement[] est;

    ExtendedStackTraceHotSpot(Throwable t) {
        super(t);
    }

    @Override
    public Iterator<ExtendedStackTraceElement> iterator() {
        return new Iterator<ExtendedStackTraceElement>() {
            private Object chunk = getBacktrace(t);
            private int j = -1;
            private int i = -1;

            @Override
            public boolean hasNext() {
                if (j + 1 >= TRACE_CHUNK_SIZE) {
                    j = -1;
                    chunk = getNext(chunk);
                    if (chunk == null)
                        return false;
                }
                return getDeclaringClass(chunk, j + 1) != null;
            }

            @Override
            public ExtendedStackTraceElement next() {
                return getStackTraceElement(getStackTraceElement0(++i), chunk, ++j);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public ExtendedStackTraceElement[] get() {
        synchronized (this) {
            if (est == null) {
                est = new ExtendedStackTraceElement[getStackTraceDepth()];
                int i = 0;
                for (ExtendedStackTraceElement e : this)
                    est[i++] = e;
            }
            return est;
        }
    }

    private int getStackTraceDepth() {
        Object chunk = getBacktrace(t);
        int depth = 0;
        if (chunk != null) {
            // Iterate over chunks and count full ones
            while (true) {
                Object next = getNext(chunk);
                if (next == null)
                    break;
                depth += TRACE_CHUNK_SIZE;
                chunk = next;
            }
            // Count element in remaining partial chunk.  NULL value for mirror
            // marks the end of the stack trace elements that are saved.
            for (int j = 0; j < TRACE_CHUNK_SIZE; j++) {
                Class<?> c = getDeclaringClass(chunk, j);
                if (c == null)
                    break;
                depth++;
            }
        }
        assert depth == getStackTraceDepth0();
        return depth;
    }

    private ExtendedStackTraceElement getStackTraceElement(int i) {
        int skipChunks = i / TRACE_CHUNK_SIZE;
        int j = i % TRACE_CHUNK_SIZE;
        Object chunk = getBacktrace(t);
        while (chunk != null && skipChunks > 0) {
            chunk = getNext(chunk);
            skipChunks--;
        }
        return getStackTraceElement(getStackTraceElement0(i), chunk, j);
    }

    private ExtendedStackTraceElement getStackTraceElement(StackTraceElement ste, Object chunk, int j) {
        return new HotSpotExtendedStackTraceElement(ste, getDeclaringClass(chunk, j), getMethod(chunk, j), getBci(chunk, j));
    }

    @Override
    protected Member getMethod(ExtendedStackTraceElement este) {
        final HotSpotExtendedStackTraceElement heste = (HotSpotExtendedStackTraceElement) este;
        Member[] ms = getMethods(heste.getDeclaringClass());
        for (Member m : ms) {
            if (heste.methodSlot == getSlot(m))
                return m;
        }
        return null;
    }

    private class HotSpotExtendedStackTraceElement extends BasicExtendedStackTraceElement {
        private final int methodSlot;

        HotSpotExtendedStackTraceElement(StackTraceElement ste, Class<?> clazz, int methodSlot, int bci) {
            super(ste, clazz, null, bci);
            this.methodSlot = methodSlot;
        }
    }

    private int getStackTraceDepth0() {
        try {
            return (Integer) getStackTraceDepth.invoke(t);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw rethrow(e);
        }
    }

    private StackTraceElement getStackTraceElement0(int i) {
        try {
            return (StackTraceElement) getStackTraceElement.invoke(t, i);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw rethrow(e);
        }
    }

    private static int getSlot(/*Executable*/Member m) {
        try {
            if (m instanceof Constructor)
                return ctorSlot.getInt((Constructor) m);
            else if (m instanceof Field)
                return fieldSlot.getInt((Field) m);
            return methodSlot.getInt((Method) m);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static Object getBacktrace(Throwable t) {
        // the JVM blocks access to Throwable.backtrace via reflection
        return (Object[]) UNSAFE.getObject(t, BACKTRACE_FIELD_OFFSET);
//        try {
//            return (Object[]) backtrace.get(t);
//        } catch (IllegalAccessException e) {
//            throw new AssertionError(e);
//        }
    }

    private static Class<?> getDeclaringClass(Object chunk, int j) {
        return (Class<?>) ((Object[]) ((Object[]) chunk)[TRACE_CLASSES_OFFSET])[j];
    }

    private static short getMethod(Object chunk, int j) {
        return ((short[]) ((Object[]) chunk)[TRACE_METHOD_OFFSET])[j];
    }

    private static short getBci(Object chunk, int j) {
        int bciAndVersion = ((int[]) ((Object[]) chunk)[TRACE_BCIS_OFFSET])[j];
        short bci = (short) (bciAndVersion >>> 16);
        short version = (short) (bciAndVersion & 0xffff);
        return bci;
    }

    private static Object getNext(Object chunk) {
        return (Object[]) ((Object[]) chunk)[((Object[]) chunk).length - 1];
    }

    // array of arrays; each content array contains trace_chunck_size elements. trace_next_offset points to next array of arrays
    private static final long BACKTRACE_FIELD_OFFSET; // 
    private static final int TRACE_METHOD_OFFSET = 0;  // shorts -- index into class's methods; should be equal to Method.slot
    private static final int TRACE_BCIS_OFFSET = 1;    // ints 
    private static final int TRACE_CLASSES_OFFSET = 2; // object array containing classes
    // private static final int TRACE_CPREFS_OFFSET = 3; // short -- index into constant pool: method name if method is null
    // private static final int TRACE_NEXT_OFFSET = 4; // this elements points to next array of arrays
    private static final int TRACE_CHUNK_SIZE = 32; // maximum num of elements in each array

    // private static final Field backtrace; // the JVM blocks access to Throwable.backtrace via reflection
    private static final Method getStackTraceDepth;
    private static final Method getStackTraceElement;
    private static final Field methodSlot;
    private static final Field ctorSlot;
    private static final Field fieldSlot;
    private static final sun.misc.Unsafe UNSAFE = UtilUnsafe.getUnsafe();

    static {
        try {
            final String javaVersion = doPrivileged(new GetProperty("java.version"));
            if (!javaVersion.startsWith("1.8") && !javaVersion.startsWith("8.") && !javaVersion.startsWith("1.9") && !javaVersion.startsWith("9."))
                throw new IllegalStateException("UnsupportedJavaVersion");
            if (!doPrivileged(new GetProperty("java.vm.name")).toLowerCase().contains("hotspot"))
                throw new IllegalStateException("Not HotSpot");
            // the JVM blocks access to Throwable.backtrace via reflection
            // backtrace = ReflectionUtil.accessible(Throwable.class.getDeclaredField("backtrace"));
            getStackTraceDepth = doPrivileged(new GetAccessDeclaredMethod(Throwable.class, "getStackTraceDepth"));
            getStackTraceElement = doPrivileged(new GetAccessDeclaredMethod(Throwable.class, "getStackTraceElement", int.class));
            methodSlot = doPrivileged(new GetAccessDeclaredField(Method.class, "slot"));
            ctorSlot = doPrivileged(new GetAccessDeclaredField(Constructor.class, "slot"));
            fieldSlot = doPrivileged(new GetAccessDeclaredField(Field.class, "slot"));

            BACKTRACE_FIELD_OFFSET = doPrivileged(new GuessBacktraceFieldOffset());

            sanityCheck();
        } catch (PrivilegedActionException e) {
            throw new AssertionError(e.getCause());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static final class GetProperty implements PrivilegedAction<String> {
        private final String name;

        GetProperty(String name) {
            this.name = name;
        }

        @Override
        public String run() {
            return System.getProperty(name);
        }
    }

    private static final class GuessBacktraceFieldOffset implements PrivilegedAction<Long> {
        @Override
        public Long run() {
            Field[] fs = Throwable.class.getDeclaredFields();
            Field second = null;
            for (Field f : fs) {
                if (getSlot(f) == 2) {
                    second = f;
                    break;
                }
            }
            if (second == null)
                throw new IllegalStateException();
            long secondOffest = UNSAFE.objectFieldOffset(second);
            if (secondOffest == 16)
                return 12L; // compressed oops
            if (secondOffest == 24)
                return 16L; // no compressed oops
            else
                throw new IllegalStateException("secondOffset: " + secondOffest); // unfamiliar
        }
    }

    private static void sanityCheck() {
        Throwable t = new Throwable();
        Object[] chunk = (Object[]) getBacktrace(t);
        if (((Object[]) chunk[TRACE_CLASSES_OFFSET]).length != TRACE_CHUNK_SIZE)
            throw new IllegalStateException();
        if (((short[]) chunk[TRACE_METHOD_OFFSET]).length != TRACE_CHUNK_SIZE)
            throw new IllegalStateException();
        if (((int[]) chunk[TRACE_BCIS_OFFSET]).length != TRACE_CHUNK_SIZE)
            throw new IllegalStateException();
        final Object[] nextChunk = (Object[]) getNext(chunk);
        if (nextChunk != null && nextChunk.length != chunk.length)
            throw new IllegalStateException();
    }
}
