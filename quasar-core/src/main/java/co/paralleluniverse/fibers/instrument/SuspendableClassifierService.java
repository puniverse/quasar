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

    public static MethodDatabase.SuspendableType isSuspendable(String className, ClassEntry classEntry, String methodName, String methodDesc, String methodSignature, String[] methodExceptions) {
        for (SuspendableClassifier sc : loader) {
            MethodDatabase.SuspendableType st = sc.isSuspendable(className, classEntry.getSuperName(), classEntry.getInterfaces(), methodName, methodDesc, methodSignature, methodExceptions);
            if (st != null)
                return st;
        }
        if (checkExceptions(methodExceptions))
            return MethodDatabase.SuspendableType.SUSPENDABLE;
        return null;
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
