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
    private static final Field targetField;
    private static final Field threadLocalsField;
    private static final Field inheritableThreadLocalsField;
    private static final Field contextClassLoaderField;
    private static final Field inheritedAccessControlContextField;
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
            targetField                  = getDeclaredFieldAndEnableAccess(Thread.class,"target");
            threadLocalsField            = getDeclaredFieldAndEnableAccess(Thread.class,"threadLocals");
            inheritableThreadLocalsField = getDeclaredFieldAndEnableAccess(Thread.class,"inheritableThreadLocals");
            contextClassLoaderField      = getDeclaredFieldAndEnableAccess(Thread.class,"contextClassLoader");

            inheritedAccessControlContextField = maybeGetDeclaredFieldAndEnableAccess(Thread.class,"inheritedAccessControlContext");

            threadLocalMapClass                = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            threadLocalMapConstructor          = getDeclaredConstructorAndEnableAccess(threadLocalMapClass,ThreadLocal.class,Object.class);
            threadLocalMapInheritedConstructor = getDeclaredConstructorAndEnableAccess(threadLocalMapClass,threadLocalMapClass);
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
    
    protected static Constructor getDeclaredConstructorAndEnableAccess(Class klass,Class<?>... parameterTypes) throws NoSuchMethodException {
        Constructor constructor = klass.getDeclaredConstructor(parameterTypes);
        
        constructor.setAccessible(true);
        
        return constructor;
    }
    
    protected static Field getDeclaredFieldAndEnableAccess(Class klass,String fieldname) throws NoSuchFieldException {
        Field field = klass.getDeclaredField(fieldname);
        
        field.setAccessible(true);
        
        return field;
    }
    
    protected static Field maybeGetDeclaredFieldAndEnableAccess(Class klass,String fieldname) {
        try {
            return getDeclaredFieldAndEnableAccess(klass,fieldname);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public static Runnable getTarget(Thread thread) {
        try {
            return (Runnable) targetField.get(thread);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static void setTarget(Thread thread, Runnable target) {
        try {
            targetField.set(thread, target);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static Object getThreadLocals(Thread thread) {
        try {
            return threadLocalsField.get(thread);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static void setThreadLocals(Thread thread, Object threadLocals) {
        try {
            threadLocalsField.set(thread, threadLocals);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static Object getInheritableThreadLocals(Thread thread) {
        try {
            return inheritableThreadLocalsField.get(thread);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static void setInheritableThreadLocals(Thread thread, Object inheritableThreadLocals) {
        try {
            inheritableThreadLocalsField.set(thread, inheritableThreadLocals);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
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
        try {
            return (ClassLoader) contextClassLoaderField.get(thread);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
   }

    public static void setContextClassLoader(Thread thread, ClassLoader classLoader) {
        try {
            contextClassLoaderField.set(thread, classLoader);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static AccessControlContext getInheritedAccessControlContext(Thread thread) {
        try {
            if (inheritedAccessControlContextField==null)
                return null;
            return (AccessControlContext) inheritedAccessControlContextField.get(thread);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static void setInheritedAccessControlContext(Thread thread, AccessControlContext accessControlContext) {
        try {
            if (inheritedAccessControlContextField!=null)
                inheritedAccessControlContextField.set(thread, accessControlContext);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
