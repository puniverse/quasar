/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2017, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.kotlin.fibers.lang

import co.paralleluniverse.fibers.SuspendExecution
import co.paralleluniverse.fibers.Suspendable

// See https://github.com/puniverse/quasar/issues/275
// @FunctionalInterface // Commented out as unnecessary and would need split for JDK8
interface KotlinSamInterface {
    @Suspendable
    @Throws(SuspendExecution::class)
    fun doSomething()
}

@Suspendable
@Throws(SuspendExecution::class)
fun accept(function: KotlinSamInterface) {
    function.doSomething()
}
