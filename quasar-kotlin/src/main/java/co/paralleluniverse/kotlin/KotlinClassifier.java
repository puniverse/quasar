/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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

/**
 * Given classes and methodRegexps, Instrumenting all the extending methods in 
 * the scope of given package prefix.
 */
public class KotlinClassifier implements SuspendableClassifier {
	private static final String PKG_PREFIX = "kotlin";
	private static final String[][] supers = {
			{"kotlin/reflect/KCallable", "call", "callBy"},
			{"kotlin/reflect/KProperty0", "get"},
			{"kotlin/reflect/KMutableProperty0", "set"},
			{"kotlin/reflect/KProperty1", "get"},
			{"kotlin/reflect/KMutableProperty1", "set"},
			{"kotlin/reflect/KProperty2", "get"},
			{"kotlin/reflect/KMutableProperty2", "set"},
			{"kotlin/jvm/functions/Function0", "invoke"},
			{"kotlin/jvm/functions/Function1", "invoke"},
			{"kotlin/jvm/functions/Function2", "invoke"},
			{"kotlin/jvm/functions/Function3", "invoke"},
			{"kotlin/jvm/functions/Function4", "invoke"},
			{"kotlin/jvm/functions/Function5", "invoke"},
			{"kotlin/jvm/functions/Function6", "invoke"},
			{"kotlin/jvm/functions/Function7", "invoke"},
			{"kotlin/jvm/functions/Function8", "invoke"},
			{"kotlin/jvm/functions/Function9", "invoke"},
			{"kotlin/jvm/functions/Function10", "invoke"},
			{"kotlin/jvm/functions/Function11", "invoke"},
			{"kotlin/jvm/functions/Function12", "invoke"},
			{"kotlin/jvm/functions/Function13", "invoke"},
			{"kotlin/jvm/functions/Function14", "invoke"},
			{"kotlin/jvm/functions/Function15", "invoke"},
			{"kotlin/jvm/functions/Function16", "invoke"},
			{"kotlin/jvm/functions/Function17", "invoke"},
			{"kotlin/jvm/functions/Function18", "invoke"},
			{"kotlin/jvm/functions/Function19", "invoke"},
			{"kotlin/jvm/functions/Function20", "invoke"},
			{"kotlin/jvm/functions/Function21", "invoke"},
			{"kotlin/jvm/functions/Function22", "invoke"},
	};

	@Override
	public MethodDatabase.SuspendableType isSuspendable (
			MethodDatabase db,
			String sourceName, String sourceDebugInfo,
			boolean isInterface, String className, String superClassName, String[] interfaces,
			String methodName, String methodDesc, String methodSignature, String[] methodExceptions
	) {
		// Declares given methods as supers
		for (String[] susExtendables : supers) {
			if (className.equals(susExtendables[0]))
				for (int i = 1; i < susExtendables.length; i++) {
					if (methodName.matches(susExtendables[i])) {
						if (db.isVerbose())
							db.getLog().log(LogLevel.INFO, KotlinClassifier.class.getName() + ": " + className + "." + methodName + " supersOrEqual " + susExtendables[0] + "." + susExtendables[i]);
						return MethodDatabase.SuspendableType.SUSPENDABLE_SUPER;
					}
				}
		}


		if (className != null && !className.startsWith(PKG_PREFIX) &&
			!(className.contains("$") && sourceName != null && sourceName.toLowerCase().endsWith(".kt")))
			return null;

		for (String[] susExtendables : supers) {
			if (SimpleSuspendableClassifier.extendsOrImplements(susExtendables[0], db, className, superClassName, interfaces))
				for (int i = 1; i < susExtendables.length; i++) {
					if (methodName.matches(susExtendables[i])) {
						if (db.isVerbose())
							db.getLog().log(LogLevel.INFO, KotlinClassifier.class.getName() + ": " + className + "." + methodName + " extends " + susExtendables[0] + "." + susExtendables[i]);
						return MethodDatabase.SuspendableType.SUSPENDABLE;
					}
				}
		}

		return null;
	}
}
