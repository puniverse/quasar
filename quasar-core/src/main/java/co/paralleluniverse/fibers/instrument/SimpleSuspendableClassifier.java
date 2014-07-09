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

import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author pron
 */
public class SimpleSuspendableClassifier implements SuspendableClassifier {
    public static final String PREFIX = "META-INF/";
    public static final String SUSPENDABLES_FILE = "suspendables";
    public static final String SUSPENDABLE_SUPERS_FILE = "suspendable-supers";
    private final Set<String> suspendables = new HashSet<String>();
    private final Set<String> suspendableClasses = new HashSet<String>();
    private final Set<String> suspendableSupers = new HashSet<String>();
    private final Set<String> suspendableSuperInterfaces = new HashSet<String>();

    public SimpleSuspendableClassifier(ClassLoader classLoader) {
        readFiles(classLoader, SUSPENDABLES_FILE, suspendables, suspendableClasses);
        readFiles(classLoader, SUSPENDABLE_SUPERS_FILE, suspendableSupers, suspendableSuperInterfaces);
    }

    SimpleSuspendableClassifier(String suspendablesFileName) {
        readSuspendablesFile(suspendablesFileName, suspendables, suspendableClasses);
    }

    Set<String> getSuspendables() {
        return suspendables;
    }

    Set<String> getSuspendableClasses() {
        return suspendableClasses;
    }

    private void readFiles(ClassLoader classLoader, String fileName, Set<String> set, Set<String> classSet) {
//        System.out.println("ZZZZ classLoader: " + classLoader);
//        if (classLoader instanceof java.net.URLClassLoader)
//            System.out.println("ZZZZ URLs: " + classLoader + " - " + Arrays.toString(((java.net.URLClassLoader) classLoader).getURLs()));

        try {
            for (Enumeration<URL> susFiles = classLoader.getResources(PREFIX + fileName); susFiles.hasMoreElements();) {
                URL file = susFiles.nextElement();
                parse(file, set, classSet);
            }
        } catch (IOException e) {
            // silently ignore
        }
    }

    private void readSuspendablesFile(String fileName, Set<String> set, Set<String> classSet) {
        try {
            parse(new File(fileName).toURI().toURL(), set, classSet);
        } catch (MalformedURLException ex) {
            throw new AssertionError(ex);
        }
    }

    private static void parse(URL file, Set<String> set, Set<String> classSet) {
        try (InputStream is = file.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
            String line;

            for (int linenum = 1; (line = reader.readLine()) != null; linenum++) {
                final String s = line.trim();
                if (s.isEmpty())
                    continue;
                if (s.charAt(0) == '#')
                    continue;
                final int index = s.lastIndexOf('.');
                if (index <= 0) {
                    System.err.println("Can't parse line " + linenum + " in " + file + ": " + line);
                    continue;
                }
                final String className = s.substring(0, index).replace('.', '/');
                final String methodName = s.substring(index + 1);
                final String fullName = className + '.' + methodName;

                if (methodName.equals("*")) {
                    if (classSet != null)
                        classSet.add(className);
                } else
                    set.add(fullName);
            }
        } catch (IOException e) {
            // silently ignore
        }
    }

    public boolean isSuspendable(String className, String methodName) {
        return (suspendables.contains(className + '.' + methodName) || suspendableClasses.contains(className));
    }

    @Override
    public SuspendableType isSuspendable(MethodDatabase db, String className, String superClassName, String[] interfaces, String methodName, String methodDesc, String methodSignature, String[] methodExceptions) {
        final String fullMethodName = className + '.' + methodName;
        if (suspendables.contains(fullMethodName))
            return SuspendableType.SUSPENDABLE;
        if (suspendableClasses.contains(className))
            return SuspendableType.SUSPENDABLE;
        if (suspendableSupers.contains(fullMethodName))
            return SuspendableType.SUSPENDABLE_SUPER;
        if (suspendableSuperInterfaces.contains(className))
            return SuspendableType.SUSPENDABLE_SUPER;

        if (superClassName != null) {
            MethodDatabase.ClassEntry ce = db.getOrLoadClassEntry(superClassName);
            if (ce != null && isSuspendable(db, superClassName, ce.getSuperName(), ce.getInterfaces(), methodName, methodDesc, methodSignature, methodExceptions) == SuspendableType.SUSPENDABLE)
                return SuspendableType.SUSPENDABLE;
        }

        if (interfaces != null) {
            for (String iface : interfaces) {
                MethodDatabase.ClassEntry ce = db.getOrLoadClassEntry(iface);
                if (ce != null && isSuspendable(db, iface, ce.getSuperName(), ce.getInterfaces(), methodName, methodDesc, methodSignature, methodExceptions) == SuspendableType.SUSPENDABLE)
                    return SuspendableType.SUSPENDABLE;
            }
        }

        return null;
    }

    public static boolean extendsOrImplements(String superOrIface, MethodDatabase db, String className, String superClassName, String[] interfaces) {
        if (superOrIface == null)
            throw new IllegalArgumentException("superOrIface is null");

        if (Objects.equals(superOrIface, superClassName))
            return true;
        for (String iface : interfaces) {
            if (Objects.equals(superOrIface, iface))
                return true;
        }

        if (extendsOrImplements(superOrIface, db, superClassName))
            return true;
        for (String iface : interfaces) {
            if (extendsOrImplements(superOrIface, db, iface))
                return true;
        }
        return false;
    }

    private static boolean extendsOrImplements(String superOrIface, MethodDatabase db, String className) {
        if (className == null)
            return false;

        MethodDatabase.ClassEntry ce = db.getOrLoadClassEntry(className);
        if (Objects.equals(superOrIface, ce.getSuperName()))
            return true;
        for (String iface : ce.getInterfaces()) {
            if (Objects.equals(superOrIface, iface))
                return true;
        }

        if (extendsOrImplements(superOrIface, db, ce.getSuperName()))
            return true;
        for (String iface : ce.getInterfaces()) {
            if (extendsOrImplements(superOrIface, db, iface))
                return true;
        }
        return false;
    }

    private static SuspendableType max(SuspendableType a, SuspendableType b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        if (a == SuspendableType.SUSPENDABLE || b == SuspendableType.SUSPENDABLE)
            return SuspendableType.SUSPENDABLE;
        if (a != b)
            throw new AssertionError("a: " + a + " b: " + b);
        return a;
    }
}
