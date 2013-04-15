/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Instrumented;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import jsr166e.ConcurrentHashMapV8;

/**
 *
 * @author pron
 */
public class Retransform {
    static volatile Instrumentation instrumentation;
    static volatile MethodDatabase db;
    static volatile Set<WeakReference<ClassLoader>> classLoaders = Collections.newSetFromMap(new ConcurrentHashMapV8<WeakReference<ClassLoader>, Boolean>());
    
    public static void retransform(Class<?> clazz) throws UnmodifiableClassException {
        instrumentation.retransformClasses(clazz);
    }
    
    public static MethodDatabase getMethodDB() {
        return db;
    }
    
    public static boolean isInstrumented(Class clazz) {
        return clazz.isAnnotationPresent(Instrumented.class);
    }
    
    public static boolean isInstrumented(String className) {
        for(Iterator<WeakReference<ClassLoader>> it = classLoaders.iterator(); it.hasNext();) {
            final WeakReference<ClassLoader> ref = it.next();
            final ClassLoader loader = ref.get();
            if(loader == null)
                it.remove();
            else {
                try {
                    if(isInstrumented(Class.forName(className, false, loader)))
                        return true;
                } catch (ClassNotFoundException ex) {
                }
            }
        }
        return false;
    }
}
