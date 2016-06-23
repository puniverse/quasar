/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
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
 * Pre-instrumentation information, relevant only for methods
 *
 * @author circlespainter
 */
@SuppressWarnings("WeakerAccess")
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SuspendableCalls {
    String FIELD_NAME_SUSPENDABLE_CALL_OFFSETS = "suspendableCallOffsets";
    @SuppressWarnings("unused") int[] suspendableCallOffsets() default {};
}
