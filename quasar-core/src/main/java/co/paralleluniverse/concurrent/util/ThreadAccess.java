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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import co.paralleluniverse.common.reflection.GetAccessDeclaredConstructor;
import co.paralleluniverse.common.reflection.GetAccessDeclaredField;
import co.paralleluniverse.common.reflection.GetDeclaredField;

import java.lang.ref.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import static java.security.AccessController.doPrivileged;

/**
 *
 * @author pron
 */
public class ThreadAccess {
    private static final VarHandle TARGET;
    private static final VarHandle THREAD_LOCALS;
    private static final VarHandle INHERITABLE_THREAD_LOCALS;
    private static final VarHandle CONTEXT_CLASS_LOADER;
    private static final VarHandle INHERITED_ACCESS_CONTROL_CONTEXT;
    
    private static final Constructor<?> threadLocalMapConstructor;
    private static final Constructor<?> threadLocalMapInheritedConstructor;
//    private static final Method threadLocalMapSet;
    private static final Field threadLocalMapTableField;
    private static final Field threadLocalMapSizeField;
    private static final Field threadLocalMapThresholdField;
    private static final Class<?> threadLocalMapEntryClass;
    private static final Constructor<?> threadLocalMapEntryConstructor;
    private static final Field threadLocalMapEntryValueField;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            l = MethodHandles.privateLookupIn(Thread.class, l);
            TARGET = l.findVarHandle(Thread.class, "target", Runnable.class); // l.unreflectVarHandle(Thread.class.getDeclaredField("target"));
            THREAD_LOCALS = l.unreflectVarHandle(doPrivileged(new GetDeclaredField(Thread.class, "threadLocals")));
            INHERITABLE_THREAD_LOCALS = l.unreflectVarHandle(doPrivileged(new GetDeclaredField(Thread.class, "inheritableThreadLocals")));
            CONTEXT_CLASS_LOADER = l.unreflectVarHandle(doPrivileged(new GetDeclaredField(Thread.class, "contextClassLoader")));

            VarHandle _inheritedAccessControlContext = null;
            try {
                _inheritedAccessControlContext = l.unreflectVarHandle(doPrivileged(new GetDeclaredField(Thread.class, "inheritedAccessControlContext")));
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (!(t instanceof NoSuchFieldException)) {
                    throw new RuntimeException(t);
                }
            }
            INHERITED_ACCESS_CONTROL_CONTEXT = _inheritedAccessControlContext;

            Class<?> threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            threadLocalMapConstructor = doPrivileged(new GetAccessDeclaredConstructor<>(threadLocalMapClass, ThreadLocal.class, Object.class));
            threadLocalMapInheritedConstructor = doPrivileged(new GetAccessDeclaredConstructor<>(threadLocalMapClass, threadLocalMapClass));
//            threadLocalMapSet = accessible(threadLocalMapClass.getDeclaredMethod("set", ThreadLocal.class, Object.class));
            threadLocalMapTableField = doPrivileged(new GetAccessDeclaredField(threadLocalMapClass, "table"));
            threadLocalMapSizeField = doPrivileged(new GetAccessDeclaredField(threadLocalMapClass, "size"));
            threadLocalMapThresholdField = doPrivileged(new GetAccessDeclaredField(threadLocalMapClass, "threshold"));

            threadLocalMapEntryClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry");
            threadLocalMapEntryConstructor = doPrivileged(new GetAccessDeclaredConstructor<>(threadLocalMapEntryClass, ThreadLocal.class, Object.class));
            threadLocalMapEntryValueField = doPrivileged(new GetAccessDeclaredField(threadLocalMapEntryClass, "value"));
        } catch (PrivilegedActionException ex) {
            throw new AssertionError(ex.getCause());
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    public static Runnable getTarget(Thread thread) {
        return (Runnable) TARGET.get(thread);
    }

    public static void setTarget(Thread thread, Runnable target) {
        TARGET.set(thread, target);
    }

    public static Object getThreadLocals(Thread thread) {
        return (Object) THREAD_LOCALS.get(thread);
    }

    public static void setThreadLocals(Thread thread, Object threadLocals) {
        THREAD_LOCALS.set(thread, threadLocals);
    }

    public static Object getInheritableThreadLocals(Thread thread) {
        return (Object) INHERITABLE_THREAD_LOCALS.get(thread);
    }

    public static void setInheritableThreadLocals(Thread thread, Object inheritableThreadLocals) {
        INHERITABLE_THREAD_LOCALS.set(thread, inheritableThreadLocals);
    }

    private static Object createThreadLocalMap(ThreadLocal tl, Object firstValue) {
        try {
            return threadLocalMapConstructor.newInstance(tl, firstValue);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    public static Object createInheritedMap(Object inheritableThreadLocals) {
        try {
            return threadLocalMapInheritedConstructor.newInstance(inheritableThreadLocals);
        } catch (ReflectiveOperationException ex) {
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
        } catch (ReflectiveOperationException ex) {
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
        return (ClassLoader) CONTEXT_CLASS_LOADER.get(thread);
    }

    public static void setContextClassLoader(Thread thread, ClassLoader classLoader) {
        CONTEXT_CLASS_LOADER.set(thread, classLoader);
    }

    public static AccessControlContext getInheritedAccessControlContext(Thread thread) {
        if (INHERITED_ACCESS_CONTROL_CONTEXT == null)
            return null;
        return (AccessControlContext) INHERITED_ACCESS_CONTROL_CONTEXT.get(thread);
    }

    public static void setInheritedAccessControlContext(Thread thread, AccessControlContext accessControlContext) {
        if (INHERITED_ACCESS_CONTROL_CONTEXT != null)
            INHERITED_ACCESS_CONTROL_CONTEXT.set(thread, accessControlContext);
    }
}
