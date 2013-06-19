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

import static co.paralleluniverse.fibers.instrument.Classes.EXCEPTION_NAME;
import co.paralleluniverse.fibers.instrument.MethodDatabase.ClassEntry;
import java.util.ServiceLoader;

/**
 *
 * @author pron
 */
class SuspendableClassifierService {
    private static ServiceLoader<SuspendableClassifier> loader = ServiceLoader.load(SuspendableClassifier.class);

    public static boolean isSuspendable(String className, ClassEntry classEntry, String methodName, String methodDesc, String methodSignature, String[] methodExceptions) {
        for (SuspendableClassifier sc : loader) {
            if (sc.isSuspendable(className, classEntry.superName, classEntry.getInterfaces(), methodName, methodDesc, methodSignature, methodExceptions))
                return true;
        }
        if (checkExceptions(methodExceptions))
            return true;
        return false;
    }

    private static boolean checkExceptions(String[] exceptions) {
        if (exceptions != null) {
            for (String ex : exceptions) {
                if (ex.equals(EXCEPTION_NAME))
                    return true;
            }
        }
        return false;
    }
}
