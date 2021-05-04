package co.paralleluniverse.kotlin.fibers.lang._1_4_x

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.fibers.StaticPropertiesTest.fiberWithVerifyInstrumentationOn
import org.junit.Assert.assertEquals
import org.junit.Test

class CallableReference {

    @Suspendable
    private fun doYield() {
        Fiber.yield()
    }

    @Suspendable
    private fun def1(a0:Int=0, s0: String="hello") : String {
        doYield()
        return if (a0 > 0) {s0} else {"wow"}
    }

    @Suspendable
    private fun apply(f: () -> String): String = f()

    @Suspendable
    private fun doNothing(f: () -> Unit) {doYield()}

    @Suspendable
    private fun doNothing1(f: (Int, String) -> Unit) {doYield()}

    @Suspendable
    private fun doNothing2(f: (Int, String, String) -> Unit) {doYield()}

    @Suspendable
    private fun doNothingVararg(x: Int, vararg y: String) {doYield()}

    @Test
    fun `reference default arguments`() {
        assertEquals("wow", ( fiberWithVerifyInstrumentationOn {
            apply(::def1)
        }))
    }

    @Test
    fun `reference Unit returning functions`() {
        fiberWithVerifyInstrumentationOn {
            doNothing(::def1)
        }
    }

    @Test
    fun `reference Unit vararg functions`() {
        fiberWithVerifyInstrumentationOn {
            doNothing1(::doNothingVararg)
            doNothing2(::doNothingVararg)
        }
    }

}