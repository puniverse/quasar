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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.Instrumented;
import java.io.PrintWriter;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 *
 * @author pron
 */
public class Retransform {
    static volatile Instrumentation instrumentation;
    static volatile MethodDatabase db;
    static volatile Set<WeakReference<ClassLoader>> classLoaders = Collections.newSetFromMap(MapUtil.<WeakReference<ClassLoader>, Boolean>newConcurrentHashMap());
    static final Set<Pair<String, String>> waivers = Collections.newSetFromMap(MapUtil.<Pair<String, String>, Boolean>newConcurrentHashMap());
    private static final CopyOnWriteArrayList<ClassLoadListener> listeners = new CopyOnWriteArrayList<ClassLoadListener>();

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

    public static MethodDatabase getMethodDB() {
        return db;
    }

    public static boolean isInstrumented(Class clazz) {
        return clazz.isAnnotationPresent(Instrumented.class);
    }

    public static boolean isInstrumented(String className) {
        for (Iterator<WeakReference<ClassLoader>> it = classLoaders.iterator(); it.hasNext();) {
            final WeakReference<ClassLoader> ref = it.next();
            final ClassLoader loader = ref.get();
            if (loader == null)
                it.remove();
            else {
                try {
                    if (isInstrumented(Class.forName(className, false, loader)))
                        return true;
                } catch (ClassNotFoundException ex) {
                }
            }
        }
        return false;
    }

    public static void addWaiver(String className, String methodName) {
        waivers.add(new Pair<String, String>(className, methodName));
    }

    public static boolean isWaiver(String className, String methodName) {
        if (className.startsWith("java.lang.reflect")
                || className.startsWith("sun.reflect")
                || (className.equals("co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable") && methodName.equals("run")))
            return true;
        return waivers.contains(new Pair<String, String>(className, methodName));
    }

    public static Boolean isSuspendable(String className, String methodName) {
        final MethodDatabase.ClassEntry ce = db.getClassEntry(className);
        if (ce == null)
            return null;
        return ce.isSuspendable(methodName);
    }

    static void beforeTransform(String className, Class clazz, byte[] data) {
        for (ClassLoadListener listener : listeners)
            listener.beforeTransform(className, clazz, data);
    }

    static void afterTransform(String className, Class clazz, byte[] data) {
        for (ClassLoadListener listener : listeners)
            listener.afterTransform(className, clazz, data);
    }

    public static void dumpClass(String className, byte[] data) {
        System.out.println("DUMP OF CLASS: " + className);
        ClassReader cr = new ClassReader(data);
        ClassVisitor cv = new TraceClassVisitor(null, new Textifier(), new PrintWriter(System.out));
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        System.out.println("=================");
    }

    public static void addClassLoadListener(ClassLoadListener listener) {
        listeners.addIfAbsent(listener);
    }

    public interface ClassLoadListener {
        void beforeTransform(String className, Class clazz, byte[] data);

        void afterTransform(String className, Class clazz, byte[] data);
    }
}
