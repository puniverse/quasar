package co.paralleluniverse.kotlin.fibers

import co.paralleluniverse.common.util.SystemProperties
import co.paralleluniverse.fibers.Fiber
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

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
    fun `simple MySuspendable annotation`() {
        StaticPropertiesTest.withVerifyInstrumentationOn {

            assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

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
    fun `simple DennisSuspendable annotation`() {
        StaticPropertiesTest.withVerifyInstrumentationOn {

            assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

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
    fun `simple MySuspendable, DennisSuspendable annotation`() {
        StaticPropertiesTest.withVerifyInstrumentationOn {

            assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

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

