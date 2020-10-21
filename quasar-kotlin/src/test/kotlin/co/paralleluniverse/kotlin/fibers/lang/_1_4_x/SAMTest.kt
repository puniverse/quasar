package co.paralleluniverse.kotlin.fibers.lang._1_4_x

import co.paralleluniverse.common.util.SystemProperties
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Fiber.sleep
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.fibers.StaticPropertiesTest.fiberWithVerifyInstrumentationOn
import org.junit.Assume
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SAMTest {

    private fun interface IntPredicate {
        @Suspendable
        fun accept(i:Int): Boolean
    }

    private class IntPredicateImpl : IntPredicate {
        @Suspendable
        override fun accept(i:Int): Boolean {
            Fiber.yield()
            return i > 0
        }
    }

    @Suspendable
    fun localLambda(a:Int) : Boolean {
        // val isPositive = (@Suspendable { num: Int -> num > 0})(a)
        return (@Suspendable { num: Int -> num > 0})(a)
    }

    @Suspendable
    fun localSAM(a:Int) : Boolean {
        val isPositive = IntPredicate @Suspendable{ Fiber.yield(); it > 0}
        return isPositive.accept(a)
    }

    @Test fun `local lambdas`(){
        // This lambda will be instrumented !!
        assertTrue { { num: Int -> num > 0}(5) }
    }

    @Test fun `local lambdas in suspendables`() {
        assertTrue(fiberWithVerifyInstrumentationOn {
            localLambda(4)
        })
        assertFalse(fiberWithVerifyInstrumentationOn {
            localLambda(-4)
        })
    }

    @Test fun `local SAM in suspendables`() {
        assertTrue(fiberWithVerifyInstrumentationOn {
            localSAM(4)
        })
        assertFalse(fiberWithVerifyInstrumentationOn {
            localSAM(-4)
        })
    }
}