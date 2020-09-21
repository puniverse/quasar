/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import java.io.PrintWriter;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author pron
 */
public class Retransform {
    static volatile Instrumentation instrumentation;
    static volatile QuasarInstrumentor instrumentor;
    static volatile Set<WeakReference<ClassLoader>> classLoaders = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    private static final CopyOnWriteArrayList<ClassLoadListener> listeners = new CopyOnWriteArrayList<>();

    public static void retransform(Class<?> clazz) throws UnmodifiableClassException {
        instrumentation.retransformClasses(clazz);
    }

    public static void redefine(Collection<ClassDefinition> classDefinitions) {
        try {
            instrumentation.redefineClasses(classDefinitions.toArray(new ClassDefinition[0]));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodDatabase getMethodDB(ClassLoader cl) {
        return instrumentor.getMethodDatabase(cl);
    }

    public static QuasarInstrumentor getInstrumentor() {
        return instrumentor;
    }

//    public static boolean isInstrumented(String className) {
//        for (Iterator<WeakReference<ClassLoader>> it = classLoaders.iterator(); it.hasNext();) {
//            final WeakReference<ClassLoader> ref = it.next();
//            final ClassLoader loader = ref.get();
//            if (loader == null)
//                it.remove();
//            else {
//                try {
//                    if (isInstrumented(Class.forName(className, false, loader)))
//                        return true;
//                } catch (ClassNotFoundException ex) {
//                }
//            }
//        }
//        return false;
//    }
    public static void addWaiver(String className, String methodName) {
        SuspendableHelper.addWaiver(className, methodName);
    }

    public static boolean isWaiver(String className, String methodName) {
        return SuspendableHelper.isWaiver(className, methodName);
    }

    public static Boolean isSuspendable(ClassLoader cl, String className, String methodName) {
        final MethodDatabase.ClassEntry ce = getMethodDB(cl).getClassEntry(className);
        if (ce == null)
            return null;
        return ce.isSuspendable(methodName);
    }

    static void beforeTransform(String className, Class<?> clazz, byte[] data) {
        for (ClassLoadListener listener : listeners)
            listener.beforeTransform(className, clazz, data);
    }

    static void afterTransform(String className, Class<?> clazz, byte[] data) {
        for (ClassLoadListener listener : listeners)
            listener.afterTransform(className, clazz, data);
    }

    public static void dumpClass(String className, byte[] data) {
        System.err.println("DUMP OF CLASS: " + className);
        ClassReader cr = new ClassReader(data);
        ClassVisitor cv = new TraceClassVisitor(null, new Textifier(), new PrintWriter(System.err));
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        System.out.println("=================");
    }

    public static void addClassLoadListener(ClassLoadListener listener) {
        listeners.addIfAbsent(listener);
    }

    public interface ClassLoadListener {
        void beforeTransform(String className, Class<?> clazz, byte[] data);

        void afterTransform(String className, Class<?> clazz, byte[] data);
    }
}
