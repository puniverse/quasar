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

import java.util.ArrayList;

public class KotlinClassifier implements SuspendableClassifier {
	private static final String PKG_PREFIX = "kotlin";
	private static final String[][] supers;

	private static String[] sa(String... elems) {
		return elems;
	}

	static {
		final ArrayList<String[]> res = new ArrayList<>();
		res.add(sa("kotlin/reflect/KCallable", "call", "callBy"));
		res.add(sa("kotlin/reflect/KProperty0", "get"));
		res.add(sa("kotlin/reflect/KMutableProperty0", "set"));
		res.add(sa("kotlin/reflect/KProperty1", "get"));
		res.add(sa("kotlin/reflect/KMutableProperty1", "set"));
		res.add(sa("kotlin/reflect/KProperty2", "get"));
		res.add(sa("kotlin/reflect/KMutableProperty2", "set"));
		for (int i = 0; i <= 22 ; i++)
			res.add(sa("kotlin/jvm/functions/Function" + i, "invoke"));
		supers = res.toArray(new String[0][0]);
	}

	@Override
	public MethodDatabase.SuspendableType isSuspendable (
			MethodDatabase db,
			String sourceName, String sourceDebugInfo,
			boolean isInterface, String className, String superClassName, String[] interfaces,
			String methodName, String methodDesc, String methodSignature, String[] methodExceptions
	) {
		for (final String[] s : supers) {
			if (className.equals(s[0]))
				for (int i = 1; i < s.length; i++) {
					if (methodName.matches(s[i])) {
						if (db.isVerbose())
							db.getLog().log(LogLevel.INFO, KotlinClassifier.class.getName() + ": " + className + "." + methodName + " supersOrEqual " + s[0] + "." + s[i]);
						return MethodDatabase.SuspendableType.SUSPENDABLE_SUPER;
					}
				}
		}

		if (className != null && !className.startsWith(PKG_PREFIX) &&
			!(className.contains("$") && sourceName != null && sourceName.toLowerCase().endsWith(".kt")))
			return null;

		for (final String[] s : supers) {
			if (SimpleSuspendableClassifier.extendsOrImplements(s[0], db, className, superClassName, interfaces))
				for (int i = 1; i < s.length; i++) {
					if (methodName.matches(s[i])) {
						if (db.isVerbose())
							db.getLog().log(LogLevel.INFO, KotlinClassifier.class.getName() + ": " + className + "." + methodName + " extends " + s[0] + "." + s[i]);
						return MethodDatabase.SuspendableType.SUSPENDABLE;
					}
				}
		}

		return null;
	}
}
