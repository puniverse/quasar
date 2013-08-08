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

import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashSet;
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
    private final Set<String> suspendableSupers = new HashSet<String>();

    public SimpleSuspendableClassifier() {
        readFiles(SUSPENDABLES_FILE, suspendables);
        readFiles(SUSPENDABLE_SUPERS_FILE, suspendableSupers);
    }

    private void readFiles(String fileName, Set<String> set) {
        try {
            for (Enumeration<URL> susFiles = ClassLoader.getSystemResources(PREFIX + fileName); susFiles.hasMoreElements();) {
                URL file = susFiles.nextElement();
                parse(file, set);
            }
        } catch (IOException e) {
            // silently ignore
        }
    }

    private void parse(URL file, Set<String> set) {
        try (InputStream is = file.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String s = line.trim();
                final int index = s.lastIndexOf('.');
                final String className = s.substring(0, index).replace('.', '/');
                final String methodName = s.substring(index + 1);
                final String fullName = className + '.' + methodName;

                set.add(fullName);
            }
        } catch (IOException e) {
            // silently ignore
        }
    }

    @Override
    public SuspendableType isSuspendable(String className, String superClassName, String[] interfaces, String methodName, String methodDesc, String methodSignature, String[] methodExceptions) {
        final String fullMethodName = className + '.' + methodName;
        if (suspendables.contains(fullMethodName))
            return SuspendableType.SUSPENDABLE;
        if (suspendableSupers.contains(fullMethodName))
            return SuspendableType.SUSPENDABLE_SUPER;
        return null;
    }
}
