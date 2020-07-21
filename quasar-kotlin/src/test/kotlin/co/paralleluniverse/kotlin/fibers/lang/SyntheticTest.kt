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

import org.junit.Assume
import org.junit.Test
import kotlin.test.assertEquals

import co.paralleluniverse.kotlin.fibers.StaticPropertiesTest

class SyntheticTest {

    private val aa = 5

    @Suspendable
    private fun defFun1(a : Int = 0) : Int {
        Fiber.yield()
        return a + aa
    }

    @Suspendable
    private fun defFun2(a : Int, b: Int = 0) : Int {
       Fiber.yield()
       return if (a > 0) a else b
    }

    @Suspendable
    private fun defFun3(a : Int=0, b: Int = 0) : Int {
        Fiber.yield()
        return if (a > 0) a else b
    }

    @Suspendable
    private fun defFun() : Int {
        return  defFun1() + defFun1(10) +
                defFun2(4) + defFun2(4,2) +
                defFun3() + defFun3(4) + defFun3(6,1);
    }

    @Test fun syntheticTest() {

        StaticPropertiesTest.withVerifyInstrumentationOn {
            Assume.assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<Any>() {
                @Suspendable
                override fun run(): Any {
                    return defFun();
                }
            }

            val actual = fiber.start().get()
            assertEquals(38, actual)
        }
    }
}
