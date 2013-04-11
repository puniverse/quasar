/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers;

import co.paralleluniverse.concurrent.util.UtilUnsafe;
import java.lang.reflect.Method;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
class ThreadAccess {
    private static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long targetOffset;
    private static final long threadLocalsOffset;
    private static final long inheritableThreadLocalsOffset;
    private static final Method createInheritedMap;

    static {
        try {
            targetOffset = unsafe.objectFieldOffset(Thread.class.getDeclaredField("target"));
            threadLocalsOffset = unsafe.objectFieldOffset(Thread.class.getDeclaredField("threadLocals"));
            inheritableThreadLocalsOffset = unsafe.objectFieldOffset(Thread.class.getDeclaredField("inheritableThreadLocals"));
            
            Method[] methods = ThreadLocal.class.getDeclaredMethods();
            Method tmp = null;
            for(Method method : methods) {
                if(method.getName().equals("createInheritedMap")) {
                    tmp = method;
                    break;
                }
            }
            createInheritedMap = tmp;
            assert createInheritedMap != null;
            createInheritedMap.setAccessible(true);
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
}
