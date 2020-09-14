package co.paralleluniverse.kotlin.fibers.lang

import co.paralleluniverse.common.util.SystemProperties
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertEquals

import co.paralleluniverse.kotlin.fibers.StaticPropertiesTest

typealias MyMap = Map<Int, Int>

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
       return a + b
    }

    @Suspendable
    private fun defFun3(a : Int=0, b: Int = 0) : Int {
        Fiber.yield()
        return a + b
    }

    @Suspendable
    private fun defFun() : Int {
        return  defFun1() + defFun1(10) +
                defFun2(4) + defFun2(4,2) +
                defFun3() + defFun3(4) + defFun3(6,1);
    }

    @Suspendable
    private fun defFunMap(m : MyMap, b : Boolean = false) : MyMap {
        Fiber.yield()
        return if (b) {
            m
        } else {
            mapOf<Int, Int>(1 to 3)
        }
    }

    @Test fun `synthetic primitve`() {

        StaticPropertiesTest.withVerifyInstrumentationOn {

            Assume.assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<Any>() {
                @Suspendable
                override fun run(): Any {
                    return defFun()
                }
            }

            val actual = fiber.start().get()
            assertEquals(41, actual)
        }
    }

    @Test fun `synthetic complex`() {
        StaticPropertiesTest.withVerifyInstrumentationOn {

            Assume.assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<MyMap>() {
                @Suspendable
                override fun run(): MyMap {
                    return defFunMap(mapOf(1 to 10, 2 to 20), true)
                }
            }
            val actual = fiber.start().get()
            assertEquals(mapOf(1 to 10, 2 to 20), actual)
        }
    }

    @Test fun `synthetic complex default`() {
        StaticPropertiesTest.withVerifyInstrumentationOn {

            Assume.assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<MyMap>() {
                @Suspendable
                override fun run(): MyMap {
                    return defFunMap(mapOf(1 to 11, 2 to 21))
                }
            }

            val actual = fiber.start().get()
            assertEquals(mapOf(1 to 3), actual)
        }
    }
}
