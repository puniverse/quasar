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
public abstract class ThreadAccess {
    public static final ThreadAccess ThreadAccess = loadThreadAccess();
    
    private static ThreadAccess loadThreadAccess() {
    	  try {
    	  	  return new JavaVMThreadAccess();
    	  } catch (AssertionError e) {
    	  	  return new DalvikVMThreadAccess();
    	  }
    }
    
    protected static Constructor getDeclaredConstructorAndEnableAccess(Class klass,Class<?>... parameterTypes) throws NoSuchMethodException {
        Constructor constructor = klass.getDeclaredConstructor(parameterTypes);
        
        constructor.setAccessible(true);
        
        return constructor;
    }
    
    protected static Method getDeclaredMethodAndEnableAccess(Class klass,String name,Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = klass.getDeclaredMethod(name,parameterTypes);
        
        method.setAccessible(true);
        
        return method;
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

    abstract public Runnable getTarget(Thread thread);

    abstract public void setTarget(Thread thread, Runnable target);

    abstract public Object getThreadLocals(Thread thread);

    abstract public void setThreadLocals(Thread thread, Object threadLocals);

    abstract public Object getInheritableThreadLocals(Thread thread);

    abstract public void setInheritableThreadLocals(Thread thread, Object inheritableThreadLocals);

    abstract public Object createInheritedMap(Object inheritableThreadLocals);

//  abstract public void set(Thread t, ThreadLocal tl, Object value);
//

    // createInheritedMap works only for InheritableThreadLocals
    abstract public Object cloneThreadLocalMap(Object orig);

    abstract public Map<ThreadLocal, Object> toMap(Object threadLocalMap);

    abstract public ClassLoader getContextClassLoader(Thread thread);

    abstract public void setContextClassLoader(Thread thread, ClassLoader classLoader);

    abstract public AccessControlContext getInheritedAccessControlContext(Thread thread);

    abstract public void setInheritedAccessControlContext(Thread thread, AccessControlContext accessControlContext);
}
