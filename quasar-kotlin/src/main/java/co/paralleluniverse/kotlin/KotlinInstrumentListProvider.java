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
import co.paralleluniverse.fibers.instrument.LogLevel;
import co.paralleluniverse.fibers.instrument.MethodDatabase;
import com.google.common.base.Predicate;

import java.util.Arrays;

import static co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;

/**
 * @author circlespainter
 */
public class KotlinInstrumentListProvider implements InstrumentListProvider {
    @Override
    public InstrumentMatcher[] getMatchList() {
        final String ktSynth = "Kotlin's built-in matchlist found suspendable Kotlin synthetic method";
        final String ktRT = "Kotlin's built-in matchlist found suspendable Kotlin synthetic method";

        return new InstrumentMatcher[] {
                mClassAndMeth(startsWithN("kotlin/ExtensionFunction"), eqN("invoke"), SuspendableType.SUSPENDABLE_SUPER, a(ktRT)),
                mClassAndMeth(startsWithN("kotlin/reflect/KExtensionFunction"), eqN("invoke"), SuspendableType.SUSPENDABLE_SUPER, a(ktRT)),
                mClassAndMeth(startsWithN("kotlin/jvm/functions/Function"), eqN("invoke"), SuspendableType.SUSPENDABLE_SUPER, a(ktRT)),
                mClassAndMeth(startsWithN("kotlin/reflect/KFunction"), eqN("invoke"), SuspendableType.SUSPENDABLE_SUPER, a(ktRT)),
                mClassAndMeth(startsWithN("kotlin/reflect/KMemberFunction"), eqN("invoke"), SuspendableType.SUSPENDABLE_SUPER, a(ktRT)),

                mClassAndMeth(and(startsWithN("kotlin/reflect/"), endsWithN("Property")), eqN("get"), SuspendableType.SUSPENDABLE_SUPER, a(ktRT)),
                mClassAndMeth(and(startsWithN("kotlin/reflect/"), endsWithN("Property")), eqN("set"), SuspendableType.SUSPENDABLE_SUPER, a(ktRT)),
                mClassAndMeth(and(startsWithN("kotlin/reflect/"), endsWithN("PropertyImpl")), eqN("get"), SuspendableType.SUSPENDABLE, a(ktRT)),
                mClassAndMeth(and(startsWithN("kotlin/reflect/"), endsWithN("PropertyImpl")), eqN("set"), SuspendableType.SUSPENDABLE, a(ktRT)),

                mClassAndMeth(eqN("kotlin/reflect/KTopLevelVariable"), eqN("get"), SuspendableType.SUSPENDABLE_SUPER, a(ktRT)),

                mSrcAndClassAndMeth(endsWithN(".kt"), containsN("$"), eqN("invoke"), SuspendableType.SUSPENDABLE, a(ktSynth))
        };
    }

    public static void log(final MethodDatabase db, final String mode, final String message, final String sourceName,
                           final boolean isInterface, final String className, final String superClassName, final String[] interfaces,
                           final String methodName, final String methodSignature) {
        db.log(LogLevel.DEBUG, "[KotlinSuspendableClassifier] %s, %s '%s: %s %s[extends %s implements %s]#%s(%s)'",
                mode, message, sourceName != null ? sourceName : "<no source>", isInterface ? "interface" : "class",
                className, superClassName != null ? superClassName : "<no class>",
                interfaces != null ? Arrays.toString(interfaces) : "<no interface>",
                methodName, nullToEmpty(methodSignature));
    }

    private static Action2<InstrumentMatcher.EvalCriteria, InstrumentMatcher.Match<SuspendableType>> a(final String msg) {
        return new Action2<InstrumentMatcher.EvalCriteria, InstrumentMatcher.Match<SuspendableType>>() {
            @Override
            public void call(final InstrumentMatcher.EvalCriteria c, final InstrumentMatcher.Match<SuspendableType> t) {
                if (t != null)
                    log(c.db, "auto", msg + " (match type: '" + t + "')", c.sourceName, c.isInterface, c.className, c.superClassName, c.interfaces, c.methodName, c.methodSignature);
            }
        };
    }

    private static InstrumentMatcher mSrcAndClassAndMeth(final Predicate<String> sourceP, final Predicate<String> classNameP,
                                                         final Predicate<String> methodNameP, final SuspendableType t,
                                                         final Action2<InstrumentMatcher.EvalCriteria, InstrumentMatcher.Match<SuspendableType>> a) {
        return new InstrumentMatcher(sourceP, null, null, classNameP, null, null, methodNameP, null, null, null, t, a);
    }

    private static InstrumentMatcher mClassAndMeth(final Predicate<String> classNameP, final Predicate<String> methodNameP, final SuspendableType t,
                                                   final Action2<InstrumentMatcher.EvalCriteria, InstrumentMatcher.Match<SuspendableType>> a) {
        return new InstrumentMatcher(null, null, null, classNameP, null, null, methodNameP, null, null, null, t, a);
    }

    private static Predicate<String> and(final Predicate<String> p1, final Predicate<String> p2) {
        return new Predicate<String>() {
            @Override
            public boolean apply(final String v) {
                return p1.apply(v) && p2.apply(v);
            }
        };
    }

    private static <X> Predicate<X> eqN(final X spec) {
        return new Predicate<X>() {
            @Override
            public boolean apply(final X v) {
                return spec == null || spec.equals(v);
            }
        };
    }

    private static Predicate<String> containsN(final String spec) {
        return new Predicate<String>() {
            @Override
            public boolean apply(final String v) {
                return spec == null || (v != null && v.contains(spec));
            }
        };
    }

    private static Predicate<String> startsWithN(final String spec) {
        return new Predicate<String>() {
            @Override
            public boolean apply(final String v) {
                return spec == null || (v != null && v.startsWith(spec));
            }
        };
    }

    private static Predicate<String> endsWithN(final String spec) {
        return new Predicate<String>() {
            @Override
            public boolean apply(final String v) {
                return spec == null || (v != null && v.endsWith(spec));
            }
        };
    }

    private static String nullToEmpty(final String s) {
        return s != null ? s : "";
    }
}
