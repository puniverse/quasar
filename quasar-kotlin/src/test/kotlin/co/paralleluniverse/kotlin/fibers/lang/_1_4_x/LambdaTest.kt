package co.paralleluniverse.kotlin.fibers.lang._1_4_x

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.fibers.StaticPropertiesTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LambdaTest {

    @Suspendable
    private fun fib(a: Int): Int {
        return { num: Int ->
            Fiber.yield()
            when (num) {
                0,1 -> { num}
                else -> { fib(num - 2) + fib(num - 1) }
            }
        }(a)
    }

    @Test
    fun `lambda fib`() {
        assertEquals(34, StaticPropertiesTest.fiberWithVerifyInstrumentationOn {
            fib(9)
        })
        assertEquals(144, StaticPropertiesTest.fiberWithVerifyInstrumentationOn {
            fib(12)
        })
    }

    @Test
    fun `local lambda positive`() {
        // This lambda will be instrumented !!
        assertTrue ( StaticPropertiesTest.fiberWithVerifyInstrumentationOn {
            {num: Int -> Fiber.yield(); num >= 0 }(5)
        })
        assertFalse ( StaticPropertiesTest.fiberWithVerifyInstrumentationOn {
            {num: Int -> Fiber.yield(); num >= 0 }(-1)
        })
    }

    @Test
    fun `local lambda even`() {
        assertTrue ( StaticPropertiesTest.fiberWithVerifyInstrumentationOn {
            {num: Int -> Fiber.yield(); num % 2 == 0 }(6)
        })
        assertFalse ( StaticPropertiesTest.fiberWithVerifyInstrumentationOn {
            {num: Int -> Fiber.yield(); num % 2 == 0 }(3)
        })
    }
}
