/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or a method as instrumented - for internal use only!
 * It must never be used in Java source code.
 *
 * @author Matthias Mann
 * @author circlespainter
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Instrumented {
    // Relevant only for classes
    String FIELD_NAME_IS_CLASS_AOT_INSTRUMENTED = "isClassAOTInstrumented";
    boolean isClassAOTInstrumented() default false;

    // Relevant only for methods
    String FIELD_NAME_IS_METHOD_INSTRUMENTATION_OPTIMIZED = "isMethodInstrumentationOptimized";
    boolean isMethodInstrumentationOptimized() default false;
    String FIELD_NAME_METHOD_START_SOURCE_LINE = "methodStartSourceLine";
    int methodStartSourceLine() default -1;
    String FIELD_NAME_METHOD_END_SOURCE_LINE = "methodEndSourceLine";
    int methodEndSourceLine() default -1;
    String FIELD_NAME_METHOD_SUSPENDABLE_CALL_SITES = "methodSuspendableCallSites";
    SuspendableCallSite[] methodSuspendableCallSites() default {};

    // See InstrumentMethod for an explanation, section emitting local vars in fully instrumented code
    String FIELD_NAME_METHOD_UNINSTRUMENTED_LOCALS_SLOTS = "methodUninstrumentedLocalsSlots";
    int[] methodUninstrumentedLocalsSlots() default 0;
}
