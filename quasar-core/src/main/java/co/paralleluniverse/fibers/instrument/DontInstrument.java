/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks a method as suspendable.
 * </p>
 * Marking a method as suspendable by declaring {@code throws SuspendExecution} is the preferable way, but using this annotation might
 * become necessary if the method implements or overrides a method which is not declared to throw {@link SuspendExecution}. In that case
 * marking the method in its declaring class/interface as potentially suspendable is necessary, and can be done in an external text file.
 * Please refer to the user manual to see how this can be done automatically.
 *
 * @author pron
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DontInstrument {
}
