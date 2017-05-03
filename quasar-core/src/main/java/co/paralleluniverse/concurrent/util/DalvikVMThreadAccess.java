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
public class DalvikVMThreadAccess extends ThreadAccess {
    private final Field targetField;
    private final Field threadLocalsField;
    private final Field inheritableThreadLocalsField;
    private final Field contextClassLoaderField;
    private final Class threadLocalMapClass;
    private final Constructor threadLocalMapConstructor;
    private final Constructor threadLocalMapInheritedConstructor;
    private final Method threadLocalMapPut;
    private final Field threadLocalMapTableField;
    private final Field[] threadLocalMapFields;

    protected DalvikVMThreadAccess() {
        try {
            targetField                        = getDeclaredFieldAndEnableAccess(Thread.class,"target");
            threadLocalsField                  = getDeclaredFieldAndEnableAccess(Thread.class,"localValues");
            inheritableThreadLocalsField       = getDeclaredFieldAndEnableAccess(Thread.class,"inheritableValues");
            contextClassLoaderField            = getDeclaredFieldAndEnableAccess(Thread.class,"contextClassLoader");

            threadLocalMapClass                = Class.forName("java.lang.ThreadLocal$Values");
            threadLocalMapConstructor          = getDeclaredConstructorAndEnableAccess(threadLocalMapClass);
            threadLocalMapInheritedConstructor = getDeclaredConstructorAndEnableAccess(threadLocalMapClass,threadLocalMapClass);
            threadLocalMapPut                  = getDeclaredMethodAndEnableAccess(threadLocalMapClass,"put",ThreadLocal.class,Object.class);
            threadLocalMapTableField           = getDeclaredFieldAndEnableAccess(threadLocalMapClass,"table");
            threadLocalMapFields               = new Field[] {
                getDeclaredFieldAndEnableAccess(threadLocalMapClass,"mask"),
                getDeclaredFieldAndEnableAccess(threadLocalMapClass,"size"),
                getDeclaredFieldAndEnableAccess(threadLocalMapClass,"tombstones"),
                getDeclaredFieldAndEnableAccess(threadLocalMapClass,"maximumLoad"),
                getDeclaredFieldAndEnableAccess(threadLocalMapClass,"clean"),
            };

        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
    
    public Runnable getTarget(Thread thread) {
        try {
            return (Runnable) targetField.get(thread);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void setTarget(Thread thread, Runnable target) {
        try {
            targetField.set(thread, target);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public Object getThreadLocals(Thread thread) {
        try {
            return threadLocalsField.get(thread);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void setThreadLocals(Thread thread, Object threadLocals) {
        try {
            threadLocalsField.set(thread, threadLocals);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public Object getInheritableThreadLocals(Thread thread) {
        try {
            return inheritableThreadLocalsField.get(thread);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void setInheritableThreadLocals(Thread thread, Object inheritableThreadLocals) {
        try {
            inheritableThreadLocalsField.set(thread, inheritableThreadLocals);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private Object createThreadLocalMap(ThreadLocal tl, Object firstValue) {
        try {
            Object threadLocalMap =  threadLocalMapConstructor.newInstance();
            
            threadLocalMapPut.invoke(threadLocalMap,tl,firstValue);
            
            return threadLocalMap;
        } catch (InvocationTargetException ex) {
            throw new AssertionError(ex);
        } catch (InstantiationException ex) {
            throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

    public Object createInheritedMap(Object inheritableThreadLocals) {
        try {
            return threadLocalMapInheritedConstructor.newInstance(inheritableThreadLocals);
        } catch (InvocationTargetException ex) {
            throw new AssertionError(ex);
        } catch (InstantiationException ex) {
            throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

    public Object cloneThreadLocalMap(Object orig) {
        try {
            Object clone = threadLocalMapConstructor.newInstance();
            
            threadLocalMapTableField.set(clone,((Object[]) threadLocalMapTableField.get(orig)).clone());
            
            for (Field threadLocalMapField : threadLocalMapFields) {
                 threadLocalMapField.set(clone,threadLocalMapField.get(orig));
            }

            return clone;
        } catch (InvocationTargetException ex) {
            throw new AssertionError(ex);
        } catch (InstantiationException ex) {
            throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

    public Map<ThreadLocal, Object> toMap(Object threadLocalMap) {
        throw new UnsupportedOperationException();
    }

    public ClassLoader getContextClassLoader(Thread thread) {
        try {
            return (ClassLoader) contextClassLoaderField.get(thread);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
   }

    public void setContextClassLoader(Thread thread, ClassLoader classLoader) {
        try {
            contextClassLoaderField.set(thread, classLoader);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public AccessControlContext getInheritedAccessControlContext(Thread thread) {
        // Not necessary on Dalvik
        return null;
    }

    public void setInheritedAccessControlContext(Thread thread, AccessControlContext accessControlContext) {
        // Not necessary on Dalvik
    }
}
