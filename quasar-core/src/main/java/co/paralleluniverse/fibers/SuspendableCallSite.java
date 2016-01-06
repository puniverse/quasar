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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Suspendable call site information in `@Instrumented`
 *
 * @author circlespainter
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SuspendableCallSite {
    String FIELD_NAME_DESC = "desc";
    String desc();

    String FIELD_NAME_SOURCE_LINE = "sourceLine";
    int sourceLine() default -1;

    String FIELD_NAME_ENTRY = "entry";
    int entry() default -1;

    String FIELD_NAME_PRE_INSTRUMENTATION_OFFSET = "preInstrumentationOffset";
    int preInstrumentationOffset() default -1;

    String FIELD_NAME_POST_INSTRUMENTATION_OFFSET = "postInstrumentationOffset";
    int postInstrumentationOffset() default -1;

    String FIELD_NAME_STACK_FRAME_OPERANDS_TYPES = "stackFrameOperandsTypes";
    String[] stackFrameOperandsTypes() default {};

    String FIELD_NAME_STACK_FRAME_LOCALS_TYPES = "stackFrameLocalsTypes";
    String[] stackFrameLocalsTypes() default {};
}
