/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.concurrent.lwthreads.instrument;

import org.objectweb.asm.Type;

/**
 * This class contains constants marking the names of the classes and methods relevant for instrumentation.
 * @author pron
 */
final class Classes {
    static final Class<?> SUSPEND_EXECUTION_CLASS = co.paralleluniverse.concurrent.lwthreads.SuspendExecution.class;
    static final Class<?> COROUTINE_CLASS = co.paralleluniverse.concurrent.lwthreads.LightweightThread.class;
    static final Class<?> STACK_CLASS = co.paralleluniverse.concurrent.lwthreads.Stack.class;
    
    static final String COROUTINE_NAME = Type.getInternalName(COROUTINE_CLASS);
    static final String YIELD_NAME = "suspend";
    
    static boolean isYieldMethod(String className, String methodName) {
        return COROUTINE_NAME.equals(className) && YIELD_NAME.equals(methodName);
    }
    
    private Classes() {
    }
}
