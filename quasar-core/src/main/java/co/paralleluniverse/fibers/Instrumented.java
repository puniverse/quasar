/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
/*
 * Copyright (c) 2008-2013, Matthias Mann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package co.paralleluniverse.fibers;

import co.paralleluniverse.fibers.instrument.Constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or a method as instrumented - for internal use only!
 * It must never be used in Java source code.
 * 
 * It optionally contains the coordinates within a method of instrumented
 * call sites and the source position of the method itself (both for
 * verification, if enabled).
 *
 * @author Matthias Mann
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Instrumented {
    // Relevant only for methods
    String FIELD_NAME_METHOD_OPTIMIZED = Constants.FIELD_NAME_METHOD_OPTIMIZED;
    boolean methodOptimized() default false;
    String FIELD_NAME_SUSPENDABLE_CALL_SITES = Constants.FIELD_NAME_SUSPENDABLE_CALL_SITES;
    int[] suspendableCallSites() default {}; // in source lines
    String FIELD_NAME_METHOD_START = Constants.FIELD_NAME_METHOD_START;
    int methodStart() default -1; // the source line of the start of the method
    String FIELD_NAME_METHOD_END = Constants.FIELD_NAME_METHOD_END;
    int methodEnd() default -1; // the source line of the end of the method
    String FIELD_NAME_SUSPENDABLE_CALL_SITE_NAMES = Constants.FIELD_NAME_SUSPENDABLE_CALL_SITE_NAMES;
    String[] suspendableCallSiteNames() default {};
    String FIELD_NAME_SUSPENDABLE_CALL_SITES_OFFSETS_AFTER_INSTR = Constants.FIELD_NAME_SUSPENDABLE_CALL_SITES_OFFSETS_AFTER_INSTR;
    int[] suspendableCallSitesOffsetsAfterInstr() default {}; // in bci
}
