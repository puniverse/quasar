package co.paralleluniverse.kotlin.fibers

import co.paralleluniverse.common.util.SystemProperties
import co.paralleluniverse.fibers.Fiber

import org.junit.Assume
import org.junit.Test
import kotlin.test.assertEquals

class AnnotationTest {

    // Custom annotation.
    @MySuspendable
    private fun fib(n : Int) : Int {
        return when (n) {
            0,1 -> {Fiber.yield(); 1}
            else -> fib(n-2) + fib(n-1)
        }
    }

    @Test
    fun `Simple MySuspendable annotation`() {
        StaticPropertiesTest.withVerifyInstrumentationOn {

            Assume.assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<Int>() {
                @MySuspendable
                override fun run() : Int  {
                    return fib(11)
                }
            }

            val actual = fiber.start().get()
            assertEquals(actual, 144)
        }
    }

    @Test
    fun `Simple DennisSuspendable annotation`() {
        StaticPropertiesTest.withVerifyInstrumentationOn {

            Assume.assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<Int>() {
                @DennisSuspendable
                override fun run() : Int  {
                    return fib(7)
                }
            }

            val actual = fiber.start().get()
            assertEquals(actual, 21)
        }
    }

    @Test
    fun `Simple MySuspendable, DennisSuspendable annotation`() {
        StaticPropertiesTest.withVerifyInstrumentationOn {

            Assume.assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<Int>() {
                @DennisSuspendable
                override fun run() : Int  {
                    return fib(5)
                }
            }

            val actual = fiber.start().get()
            assertEquals(actual, 8)
        }
    }
}

