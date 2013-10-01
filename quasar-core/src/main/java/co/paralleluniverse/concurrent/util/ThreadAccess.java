/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
public class ThreadAccess {
    private static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long targetOffset;
    private static final long threadLocalsOffset;
    private static final long inheritableThreadLocalsOffset;
    private static final long contextClassLoaderOffset;
    private static final Method createInheritedMap;
    private static final Class threadLocalMapClass;
    private static final Constructor threadLocalMapConstructor;
    private static final Field threadLocalMapTableField;
    private static final Field threadLocalMapSizeField;
    private static final Field threadLocalMapThresholdField;
    private static final Class threadLocalMapEntryClass;
    private static final Constructor threadLocalMapEntryConstructor;
    private static final Field threadLocalMapEntryValueField;

    static {
        try {
            targetOffset = unsafe.objectFieldOffset(Thread.class.getDeclaredField("target"));
            threadLocalsOffset = unsafe.objectFieldOffset(Thread.class.getDeclaredField("threadLocals"));
            inheritableThreadLocalsOffset = unsafe.objectFieldOffset(Thread.class.getDeclaredField("inheritableThreadLocals"));
            contextClassLoaderOffset = unsafe.objectFieldOffset(Thread.class.getDeclaredField("contextClassLoader"));

            threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            createInheritedMap = ThreadLocal.class.getDeclaredMethod("createInheritedMap", threadLocalMapClass);
            createInheritedMap.setAccessible(true);
            threadLocalMapConstructor = threadLocalMapClass.getDeclaredConstructor(ThreadLocal.class, Object.class);
            threadLocalMapConstructor.setAccessible(true);
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
        return (Runnable) unsafe.getObject(thread, targetOffset);
    }

    public static void setTarget(Thread thread, Runnable target) {
        unsafe.putObject(thread, targetOffset, target);
    }

    public static Object getThreadLocals(Thread thread) {
        return unsafe.getObject(thread, threadLocalsOffset);
    }

    public static void setThreadLocals(Thread thread, Object threadLocals) {
        unsafe.putObject(thread, threadLocalsOffset, threadLocals);
    }

    public static Object getInheritableThreadLocals(Thread thread) {
        return unsafe.getObject(thread, inheritableThreadLocalsOffset);
    }

    public static void setInheritablehreadLocals(Thread thread, Object inheritableThreadLocals) {
        unsafe.putObject(thread, inheritableThreadLocalsOffset, inheritableThreadLocals);
    }

    public static Object createInheritedMap(Object inheritableThreadLocals) {
        try {
            return createInheritedMap.invoke(null, inheritableThreadLocals);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    // createInheritedMap works only for InheritableThreadLocals
    public static Object cloneThreadLocalMap(Object orig) {
        try {
            Object clone = threadLocalMapConstructor.newInstance(new ThreadLocal(), null);

            Object origTable = threadLocalMapTableField.get(orig);
            final int len = Array.getLength(origTable);
            Object tableClone = Array.newInstance(threadLocalMapEntryClass, len);
            for (int i = 0; i < len; i++) {
                Object entry = Array.get(origTable, i);
                if(entry != null)
                    Array.set(tableClone, i, cloneThreadLocalMapEntry(entry));
            }
            
            threadLocalMapTableField.set(clone, tableClone);
            threadLocalMapSizeField.setInt(clone, threadLocalMapSizeField.getInt(orig));
            threadLocalMapThresholdField.setInt(clone, threadLocalMapThresholdField.getInt(orig));
            return clone;
        } catch (Exception ex) {
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

    public static ClassLoader getContextClassLoader(Thread thread) {
        return (ClassLoader) unsafe.getObject(thread, contextClassLoaderOffset);
    }

    public static void setContextClassLoader(Thread thread, ClassLoader classLoader) {
        unsafe.putObject(thread, contextClassLoaderOffset, classLoader);
    }
}
