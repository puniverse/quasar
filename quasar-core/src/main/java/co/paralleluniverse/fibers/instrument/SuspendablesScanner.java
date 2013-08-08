/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.Suspendable;
import static co.paralleluniverse.fibers.instrument.SimpleSuspendableClassifier.PREFIX;
import static co.paralleluniverse.fibers.instrument.SimpleSuspendableClassifier.SUSPENDABLE_SUPERS_FILE;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author pron
 */
public class SuspendablesScanner {
    private static final String BUILD_DIR = "build/";
    private static final String RESOURCES_DIR = "/resources/main/";
    private static final String CLASSES_DIR = "/classes/main/";
    private static final String CLASSFILE_SUFFIX = ".class";

    public static void main(String args[]) throws Exception {
        String classPrefix = args[0];
        String outputFile = args.length > 1 ? args[1] : BUILD_DIR + RESOURCES_DIR + PREFIX + SUSPENDABLE_SUPERS_FILE;

        run(classPrefix, outputFile);
    }

    private static void run(String prefix, String outputFile) throws Exception {
        Set<String> results = new HashSet<String>();
        collect(prefix, results);
        outputResults(results, outputFile);
    }

    private static Set<String> collect(String prefix, Set<String> results) throws Exception {
        prefix = prefix.trim();
        prefix = prefix.replace('.', '/');
        for (Enumeration<URL> urls = ClassLoader.getSystemResources(prefix); urls.hasMoreElements();) {
            URL url = urls.nextElement();
            File file = new File(url.getFile());
            if (file.isDirectory())
                scanClasses(file, results);
        }
        return results;
    }

    private static void outputResults(Set<String> results, String outputFile) throws Exception {
        if (outputFile != null) {
            outputFile = outputFile.trim();
            if (outputFile.isEmpty())
                outputFile = null;
        }
        PrintStream out = outputFile != null ? new PrintStream(outputFile) : System.out;

        List<String> sorted = new ArrayList<String>(results);
        Collections.sort(sorted);
        for (String s : sorted)
            out.println(s);
    }

    private static void scanClasses(File file, Set<String> results) throws Exception {
        if (file.isDirectory()) {
            for (File f : file.listFiles())
                scanClasses(f, results);
        } else {
            String className = extractClassName(file);
            if (className != null)
                scanClass(className, results);
        }
    }

    private static String extractClassName(File file) {
        String fileName = file.getPath();
        if (fileName.endsWith(CLASSFILE_SUFFIX) && fileName.indexOf(CLASSES_DIR) >= 0) {
            String className = fileName.substring(fileName.indexOf(CLASSES_DIR) + CLASSES_DIR.length(),
                    fileName.length() - CLASSFILE_SUFFIX.length()).replace('/', '.');
            return className;
        } else
            return null;
    }

    private static void scanClass(String className, Set<String> results) throws Exception {
        Class cls = Class.forName(className);
        Method[] methods = cls.getDeclaredMethods();
        for (Method m : methods) {
            if (m.isAnnotationPresent(Suspendable.class))
                findSuperDeclarations(cls, m, results);
        }
    }

    private static void findSuperDeclarations(Class cls, Method method, Set<String> results) {
        if (cls == null)
            return;

        if (!cls.equals(method.getDeclaringClass())) {
            try {
                cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
                results.add(cls.getName() + '.' + method.getName());
            } catch (NoSuchMethodException e) {
            }
        }

        // recursively look in superclass and interfaces
        findSuperDeclarations(cls.getSuperclass(), method, results);
        for (Class iface : cls.getInterfaces())
            findSuperDeclarations(iface, method, results);
    }
}
