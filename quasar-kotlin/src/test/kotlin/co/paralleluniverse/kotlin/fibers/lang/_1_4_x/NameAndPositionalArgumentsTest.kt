package co.paralleluniverse.kotlin.fibers.lang._1_4_x

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.fibers.StaticPropertiesTest.fiberWithVerifyInstrumentationOn
import org.junit.Assert.assertEquals
import org.junit.Test

class NameAndPositionalArgumentsTest {

    @Suspendable
    private fun doYield() {
        Fiber.yield()
    }

    @Suspendable
    private fun fun1(b0: Boolean, a0: String="bobble", a1: String="cheese") : String {
        doYield()
        return if (b0) {
            a0
        } else {
            a1
        }
    }

    @Test
    fun `local lambdas in suspendables`() {
        assertEquals("bobble", (fiberWithVerifyInstrumentationOn {
            fun1(true, a0="bobble", "kipper")
        }))
    }
}