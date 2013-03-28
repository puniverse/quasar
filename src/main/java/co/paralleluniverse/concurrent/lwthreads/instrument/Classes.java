/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.concurrent.lwthreads.instrument;

/**
 *
 * @author pron
 */
public final class Classes {
    public static Class<?> getSuspendExecutionClass() {
        return co.paralleluniverse.concurrent.lwthreads.SuspendExecution.class;
    }
    
    public static Class<?> getCoroutineClass() {
        return co.paralleluniverse.concurrent.lwthreads.LightweightThread.class;
    }
    
    public static Class<?> getStackClass() {
        return co.paralleluniverse.concurrent.lwthreads.Stack.class;
    }
    private Classes() {
    }
}
