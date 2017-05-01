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
package co.paralleluniverse.concurrent.util;

import co.paralleluniverse.common.util.*;
import sun.misc.*;

import java.lang.ref.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

/**
 *
 * @author pron
 */
public class ThreadAccess {
    private static final Unsafe UNSAFE = UtilUnsafe.getUnsafe();
    private static final long targetOffset;
    private static final long threadLocalsOffset;
    private static final long inheritableThreadLocalsOffset;
    private static final long contextClassLoaderOffset;
    private static final long inheritedAccessControlContextOffset;
    private static final Class threadLocalMapClass;
    private static final Constructor threadLocalMapConstructor;
    private static final Constructor threadLocalMapInheritedConstructor;
//    private static final Method threadLocalMapSet;
    private static final Field threadLocalMapTableField;
    private static final Field threadLocalMapSizeField;
    private static final Field threadLocalMapThresholdField;
    private static final Class threadLocalMapEntryClass;
    private static final Constructor threadLocalMapEntryConstructor;
    private static final Field threadLocalMapEntryValueField;

    static {
        try {
            targetOffset = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("target"));
            threadLocalsOffset = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("threadLocals"));
            inheritableThreadLocalsOffset = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("inheritableThreadLocals"));
            contextClassLoaderOffset = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("contextClassLoader"));

            long _inheritedAccessControlContextOffset = -1;
            try {
                _inheritedAccessControlContextOffset = UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("inheritedAccessControlContext"));
            } catch (NoSuchFieldException e) {
            }
            inheritedAccessControlContextOffset = _inheritedAccessControlContextOffset;

            threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            threadLocalMapConstructor = threadLocalMapClass.getDeclaredConstructor(ThreadLocal.class, Object.class);
            threadLocalMapConstructor.setAccessible(true);
            threadLocalMapInheritedConstructor = threadLocalMapClass.getDeclaredConstructor(threadLocalMapClass);
            threadLocalMapInheritedConstructor.setAccessible(true);
//            threadLocalMapSet = threadLocalMapClass.getDeclaredMethod("set", ThreadLocal.class, Object.class);
//            threadLocalMapSet.setAccessible(true);
            threadLocalMapTableField = threadLocalMapClass.getDeclaredField("table");
            threadLocalMapTableField.setAccessible(true);
            threadLocalMapSizeField = threadLocalMapClass.getDeclaredField("size");
            threadLocalMapSizeField.setAccessible(true);
            threadLocalMapThresholdField = threadLocalMapClass.getDeclaredField("threshold");
            threadLocalMapThresholdField.setAccessible(true);

            threadLocalMapEntryClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry");
            threadLocalMapEntryConstructor = threadLocalMapEntryClass.getDeclaredConstructor(ThreadLocal.class, Object.class);
            threadLocalMapEntryConstructor.setAccessible(true);
            threadLocalMapEntryValueField = threadLocalMapEntryClass.getDeclaredField("value");
            threadLocalMapEntryValueField.setAccessible(true);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    public static Runnable getTarget(Thread thread) {
        return (Runnable) UNSAFE.getObject(thread, targetOffset);
    }

    public static void setTarget(Thread thread, Runnable target) {
        UNSAFE.putObject(thread, targetOffset, target);
    }

    public static Object getThreadLocals(Thread thread) {
        return UNSAFE.getObject(thread, threadLocalsOffset);
    }

    public static void setThreadLocals(Thread thread, Object threadLocals) {
        UNSAFE.putObject(thread, threadLocalsOffset, threadLocals);
    }

    public static Object getInheritableThreadLocals(Thread thread) {
        return UNSAFE.getObject(thread, inheritableThreadLocalsOffset);
    }

    public static void setInheritableThreadLocals(Thread thread, Object inheritableThreadLocals) {
        UNSAFE.putObject(thread, inheritableThreadLocalsOffset, inheritableThreadLocals);
    }

    private static Object createThreadLocalMap(ThreadLocal tl, Object firstValue) {
        try {
            return threadLocalMapConstructor.newInstance(tl, firstValue);
//      } catch (ReflectiveOperationException ex) { // ReflectiveOperationException is not accessible in JDK 1.6, so we need to list InvocationTargetException, InstantiationException, IllegalAccessException individually
//          throw new AssertionError(ex);
        } catch (InvocationTargetException ex) {
            throw new AssertionError(ex);
        } catch (InstantiationException ex) {
            throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

    public static Object createInheritedMap(Object inheritableThreadLocals) {
        try {
            return threadLocalMapInheritedConstructor.newInstance(inheritableThreadLocals);
//      } catch (ReflectiveOperationException ex) { // ReflectiveOperationException is not accessible in JDK 1.6, so we need to list InvocationTargetException, InstantiationException, IllegalAccessException individually
//          throw new AssertionError(ex);
        } catch (InvocationTargetException ex) {
            throw new AssertionError(ex);
        } catch (InstantiationException ex) {
            throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

//    public static void set(Thread t, ThreadLocal tl, Object value) {
//        Object map = getThreadLocals(t);
//        if (map != null)
//            set(map, tl, value);
//        else
//            setThreadLocals(t, createThreadLocalMap(tl, value));
//    }
//
//    private static void set(Object map, ThreadLocal tl, Object value) {
//        try {
//            threadLocalMapSet.invoke(map, tl, value);
//        } catch (ReflectiveOperationException e) {
//            throw new AssertionError(e);
//        }
//    }

    // createInheritedMap works only for InheritableThreadLocals
    public static Object cloneThreadLocalMap(Object orig) {
        try {
            Object clone = createThreadLocalMap(new ThreadLocal(), null);

            Object origTable = threadLocalMapTableField.get(orig);
            final int len = Array.getLength(origTable);
            Object tableClone = Array.newInstance(threadLocalMapEntryClass, len);
            for (int i = 0; i < len; i++) {
                Object entry = Array.get(origTable, i);
                if (entry != null)
                    Array.set(tableClone, i, cloneThreadLocalMapEntry(entry));
            }

            threadLocalMapTableField.set(clone, tableClone);
            threadLocalMapSizeField.setInt(clone, threadLocalMapSizeField.getInt(orig));
            threadLocalMapThresholdField.setInt(clone, threadLocalMapThresholdField.getInt(orig));
            return clone;
//      } catch (ReflectiveOperationException ex) { // ReflectiveOperationException is not accessible in JDK 1.6, but actually just IllegalAccessException is declared to be thrown.   
//          throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

    private static Object cloneThreadLocalMapEntry(Object entry) {
        try {
            final ThreadLocal key = ((Reference<ThreadLocal>) entry).get();
            final Object value = threadLocalMapEntryValueField.get(entry);
            return threadLocalMapEntryConstructor.newInstance(key, value);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    public static Map<ThreadLocal, Object> toMap(Object threadLocalMap) {
        try {
            final Map<ThreadLocal, Object> map = new HashMap<>();
            Object table = threadLocalMapTableField.get(threadLocalMap);
            final int len = Array.getLength(table);
            for (int i = 0; i < len; i++) {
                Object entry = Array.get(table, i);
                if (entry != null)
                    map.put(((Reference<ThreadLocal>) entry).get(), threadLocalMapEntryValueField.get(entry));
            }
            return map;
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    public static ClassLoader getContextClassLoader(Thread thread) {
        return (ClassLoader) UNSAFE.getObject(thread, contextClassLoaderOffset);
    }

    public static void setContextClassLoader(Thread thread, ClassLoader classLoader) {
        UNSAFE.putObject(thread, contextClassLoaderOffset, classLoader);
    }

    public static AccessControlContext getInheritedAccessControlContext(Thread thread) {
        if (inheritedAccessControlContextOffset < 0)
            return null;
        return (AccessControlContext) UNSAFE.getObject(thread, inheritedAccessControlContextOffset);
    }

    public static void setInheritedAccessControlContext(Thread thread, AccessControlContext accessControlContext) {
        if (inheritedAccessControlContextOffset >= 0)
            UNSAFE.putObject(thread, inheritedAccessControlContextOffset, accessControlContext);
    }
}
