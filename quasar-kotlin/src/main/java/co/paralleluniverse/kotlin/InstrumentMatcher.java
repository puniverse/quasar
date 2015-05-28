/*
 * Pulsar: lightweight threads and Erlang-like actors for Clojure.
 * Copyright (C) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.kotlin;

import co.paralleluniverse.common.util.Action2;
import co.paralleluniverse.fibers.instrument.MethodDatabase;
import com.google.common.base.Predicate;

/**
 * @author circlespainter
 */
public class InstrumentMatcher {
    final Predicate<String> sourceNameP;
    final Predicate<String> sourceDebugInfoP;
    final Predicate<Boolean> isInterfaceP;
    final Predicate<String> classNameP;
    final Predicate<String> superClassNameP;
    final Predicate<String[]> interfacesP;
    final Predicate<String> methodNameP;
    final Predicate<String> methodDescP;
    final Predicate<String> methodSignatureP;
    final Predicate<String[]> methodExceptionsP;
    final MethodDatabase.SuspendableType suspendableType;
    final Action2<EvalCriteria, Match<MethodDatabase.SuspendableType>> action;

    InstrumentMatcher(final Predicate<String> sourceNameP, final Predicate<String> sourceDebugInfoP,
                      final Predicate<Boolean> isInterfaceP, final Predicate<String> classNameP, final Predicate<String> superClassNameP, final Predicate<String[]> interfacesP,
                      final Predicate<String> methodNameP, final Predicate<String> methodDescP, final Predicate<String> methodSignatureP, final Predicate<String[]> methodExceptionsP,
                      final MethodDatabase.SuspendableType suspendableType, final Action2<EvalCriteria, Match<MethodDatabase.SuspendableType>> action) {
        this.sourceNameP = sourceNameP;
        this.sourceDebugInfoP = sourceDebugInfoP;
        this.isInterfaceP = isInterfaceP;
        this.classNameP = classNameP;
        this.superClassNameP = superClassNameP;
        this.interfacesP = interfacesP;
        this.methodNameP = methodNameP;
        this.methodDescP = methodDescP;
        this.methodSignatureP = methodSignatureP;
        this.methodExceptionsP = methodExceptionsP;
        this.suspendableType = suspendableType;
        this.action = action;
    }

    Match<MethodDatabase.SuspendableType> eval(final MethodDatabase db, final String sourceName, final String sourceDebugInfo,
                         final boolean isInterface, final String className, final String superClassName, final String[] interfaces,
                         final String methodName, final String methodDesc, final String methodSignature, final String[] methodExceptions) {
        final Match<MethodDatabase.SuspendableType> ret =
                (sourceNameP == null || sourceNameP.apply(sourceName))
                && (sourceDebugInfoP == null || sourceDebugInfoP.apply(sourceDebugInfo))
                && (isInterfaceP == null || isInterfaceP.apply(isInterface))
                && (classNameP == null || classNameP.apply(className))
                && (superClassNameP == null || superClassNameP.apply(superClassName))
                && (interfacesP == null || interfacesP.apply(interfaces))
                && (methodNameP == null || methodNameP.apply(methodName))
                && (methodDescP == null || methodDescP.apply(methodDesc))
                && (methodSignatureP == null || methodSignatureP.apply(methodSignature))
                && (methodExceptionsP == null || methodExceptionsP.apply(methodExceptions))
            ? new Match<>(suspendableType) : null;
        action.call(new EvalCriteria(db, sourceName, sourceDebugInfo, isInterface, className, superClassName, interfaces, methodName, methodDesc, methodSignature, methodExceptions), ret);
        return ret;
    }

    public class Match<T> {
        private final T value;

        public Match(T v) {
            this.value = v;
        }

        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Match{" +
                    "value=" + (value != null ? value : "<no comment>") +
                    '}';
        }
    }

    public class EvalCriteria {
        final MethodDatabase db;
        final String sourceName;
        final String sourceDebugInfo;
        final boolean isInterface;
        final String className;
        final String superClassName;
        final String[] interfaces;
        final String methodName;
        final String methodDesc;
        final String methodSignature;
        final String[] methodExceptions;

        public EvalCriteria(final MethodDatabase db, final String sourceName, final String sourceDebugInfo,
                            final boolean isInterface, final String className, final String superClassName, final String[] interfaces,
                            final String methodName, final String methodDesc, final String methodSignature, final String[] methodExceptions) {
            this.db = db;
            this.sourceName = sourceName;
            this.sourceDebugInfo = sourceDebugInfo;
            this.isInterface = isInterface;
            this.className = className;
            this.superClassName = superClassName;
            this.interfaces = interfaces;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.methodSignature = methodSignature;
            this.methodExceptions = methodExceptions;
        }
    }
}
