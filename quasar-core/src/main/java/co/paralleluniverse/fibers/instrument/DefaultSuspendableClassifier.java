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
import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import java.util.ServiceLoader;

/**
 *
 * @author pron
 */
class DefaultSuspendableClassifier implements SuspendableClassifier {
    private final ServiceLoader<SuspendableClassifier> loader;
    private final SuspendableClassifier simpleClassifier;

    public DefaultSuspendableClassifier(ClassLoader classLoader) {
        this.loader = ServiceLoader.load(SuspendableClassifier.class, classLoader);
        this.simpleClassifier = new SimpleSuspendableClassifier(classLoader);
    }

    @Override
    public SuspendableType isSuspendable(String className, String superClassName, String[] interfaces, String methodName, String methodDesc, String methodSignature, String[] methodExceptions) {
        SuspendableType st;

        // simple classifier (files in META-INF)
        st = simpleClassifier.isSuspendable(className, superClassName, interfaces, methodName, methodDesc, methodSignature, methodExceptions);
        if (st != null)
            return st;

        // classifier service
        for (SuspendableClassifier sc : loader) {
            st = sc.isSuspendable(className, superClassName, interfaces, methodName, methodDesc, methodSignature, methodExceptions);
            if (st != null)
                return st;
        }

        // throws SuspendExceution
        if (checkExceptions(methodExceptions))
            return SuspendableType.SUSPENDABLE;
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
