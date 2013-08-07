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
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author pron
 */
public class SuspendablesScanner {
    private static final String CLASSES_DIR = "/classes/main/";
    private static final String CLASSFILE_SUFFIX = ".class";
    
    public static void main(String args[]) throws Exception {
        collect(args[0]);
    }
    
    private static void collect(String prefix) throws Exception {
        Set<String> results = new HashSet<String>();
        prefix = prefix.replace('.', '/');
        for (Enumeration<URL> urls = ClassLoader.getSystemResources(prefix); urls.hasMoreElements();) {
            URL url = urls.nextElement();
            File file = new File(url.getFile());
            if (file.isDirectory())
                scanClasses(file, results);
        }
        
        for (String s : results)
            System.out.println(s);
    }
    
    private static void scanClasses(File file, Set<String> results) throws Exception {
        if (file.isDirectory()) {
            for (File f : file.listFiles())
                scanClasses(f, results);
        } else {
            String fileName = file.getPath();
            if (fileName.endsWith(CLASSFILE_SUFFIX)) {
                String className = fileName.substring(fileName.indexOf(CLASSES_DIR) + CLASSES_DIR.length(),
                        fileName.length() - CLASSFILE_SUFFIX.length()).replace('/', '.');
                scanClass(className, results);
            }
        }
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
                Method m = cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
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
