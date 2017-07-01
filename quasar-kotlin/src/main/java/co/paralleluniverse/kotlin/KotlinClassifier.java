/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015-2017, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.instrument.LogLevel;
import co.paralleluniverse.fibers.instrument.MethodDatabase;
import co.paralleluniverse.fibers.instrument.SimpleSuspendableClassifier;
import co.paralleluniverse.fibers.instrument.SuspendableClassifier;

import java.util.ArrayList;

/**
 * Quasar-Kotlin M14 integration.
 *
 * @author circlespainter
 */
public class KotlinClassifier implements SuspendableClassifier {
    private static final String PKG_PREFIX = "kotlin";
    private static final String[][] supers;
    private static final String[] excludePrefixes;

    static {
        final ArrayList<String[]> supersList = new ArrayList<>();

        // Kotlin properties reflection support
        supersList.add(sa("kotlin/reflect/KCallable", "call", "callBy"));
        supersList.add(sa("kotlin/reflect/full/KCallable", "call", "callBy"));

        supersList.add(sa("kotlin/reflect/KProperty0", "get"));
        supersList.add(sa("kotlin/reflect/full/KProperty0", "get"));
        supersList.add(sa("kotlin/reflect/KMutableProperty0", "set"));
        supersList.add(sa("kotlin/reflect/full/KMutableProperty0", "set"));

        supersList.add(sa("kotlin/reflect/KProperty1", "get"));
        supersList.add(sa("kotlin/reflect/full/KProperty1", "get"));
        supersList.add(sa("kotlin/reflect/KMutableProperty1", "set"));
        supersList.add(sa("kotlin/reflect/full/KMutableProperty1", "set"));

        supersList.add(sa("kotlin/reflect/KProperty2", "get"));
        supersList.add(sa("kotlin/reflect/full/KProperty2", "get"));
        supersList.add(sa("kotlin/reflect/KMutableProperty2", "set"));
        supersList.add(sa("kotlin/reflect/full/KMutableProperty2", "set"));

        supersList.add(sa("kotlin/reflect/KObservableProperty", "beforeChange", "afterChange"));
        supersList.add(sa("kotlin/reflect/full/KObservableProperty", "beforeChange", "afterChange"));

        // Observable properties
        supersList.add(sa("kotlin/properties/ObservableProperty", "beforeChange", "afterChange"));
        supersList.add(sa("kotlin/properties/ReadWriteProperty", "getValue", "setValue"));

        // Kotlin Lazy
        supersList.add(sa("kotlin/Lazy", "getValue"));

        // Kotlin functions support
        for (int i = 0; i <= 22; i++)
            supersList.add(sa("kotlin/jvm/functions/Function" + i, "invoke"));

        // Kotlin M14 doesn't seem to add `@Suspendable` to the generated `run` when passing a `@Suspendable` lambda
        supersList.add(sa("co/paralleluniverse/strands/SuspendableCallable", "run"));
        supersList.add(sa("co/paralleluniverse/strands/SuspendableRunnable", "run"));

        supers = supersList.toArray(new String[0][0]);


        // Class prefixes that are known not to suspend
        excludePrefixes = new String[] {
            // TODO: this specifically is also known to cause a `VerifyError` when instrumented, see #146
            "kotlin/reflect/jvm/internal/impl/descriptors/impl/ModuleDescriptorImpl",
            // Handle the same class, when shaded within kotlin-compiler[-embeddable]
            "org/jetbrains/kotlin/descriptors/impl/ModuleDescriptorImpl"
        };
    }

    @Override
    public MethodDatabase.SuspendableType isSuspendable (
        MethodDatabase db,
        String sourceName, String sourceDebugInfo,
        boolean isInterface, String className, String superClassName, String[] interfaces,
        String methodName, String methodDesc, String methodSignature, String[] methodExceptions
    ) {
        if (className == null)
            return null;

        for (final String[] s : supers) {
            if (className.equals(s[0]))
                for (int i = 1; i < s.length; i++) {
                    if (methodName != null && methodName.matches(s[i])) {
                        if (db.isVerbose())
                            db.getLog().log(LogLevel.INFO, KotlinClassifier.class.getName() + ": " + className + "." + methodName + " supersOrEqual " + s[0] + "." + s[i]);
                        return MethodDatabase.SuspendableType.SUSPENDABLE_SUPER;
                    }
                }
        }

        // Don't consider Kotlin user files without inner classes
        if (!className.startsWith(PKG_PREFIX)
            && !(className.contains("$") && sourceName != null && sourceName.toLowerCase().endsWith(".kt")))
            return null;

        // Exclude packages known not to suspend
        for (final String s : excludePrefixes) {
            if (className.startsWith(s))
                return null;
        }

        for (final String[] s : supers) {
            if (SimpleSuspendableClassifier.extendsOrImplements(s[0], db, className, superClassName, interfaces))
                for (int i = 1; i < s.length; i++) {
                    if (methodName != null && methodName.matches(s[i])) {
                        if (db.isVerbose())
                            db.getLog().log(LogLevel.INFO, KotlinClassifier.class.getName() + ": " + className + "." + methodName + " extends " + s[0] + "." + s[i]);
                        return MethodDatabase.SuspendableType.SUSPENDABLE;
                    }
                }
        }

        // Java7 compilation scheme
        if (methodName != null && methodName.contains("access$"))
            return MethodDatabase.SuspendableType.SUSPENDABLE;

        return null;
    }

    private static String[] sa(String... elems) {
        return elems;
    }
}
