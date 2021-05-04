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
package co.paralleluniverse.kotlin.fibers.lang

import co.paralleluniverse.common.util.SystemProperties
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.fibers.StaticPropertiesTest
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

class MethodOverloadTest {

    @Suspendable
    fun function() {
        function(arrayOf(0))
    }

    @Suspendable
    fun function(m: Any) {
        Fiber.yield()
    }

    @Test fun methodOverloadTest() {

        StaticPropertiesTest.withVerifyInstrumentationOn {
            assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<Any>() {
                @Suspendable
                override fun run(): Any {
                    return function(object {})
                }
            }

            val actual = fiber.start().get()
            assertNotNull(actual)
        }
    }
}
