package co.paralleluniverse.kotlin.fibers.lang._1_4_x

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.kotlin.fibers.StaticPropertiesTest
import co.paralleluniverse.fibers.Suspendable
import org.junit.Assert.assertEquals
import org.junit.Test

class BreakContinueTest {

    @Suspendable
    private fun continueBreak(i:Int) : Int {
        Fiber.yield()
        var n = 0
        for (x in 1..i) {
            when (x) {
                5,7 -> continue
                20 -> break
                else -> n++
            }
            Fiber.yield()
        }
        return n
    }

    @Test
    fun `continue break`() {
        assertEquals(4, StaticPropertiesTest.fiberWithVerifyInstrumentationOn {
            continueBreak(4)
        })
        assertEquals(5, StaticPropertiesTest.fiberWithVerifyInstrumentationOn {
            continueBreak(6)
        })
        assertEquals(7, StaticPropertiesTest.fiberWithVerifyInstrumentationOn {
            continueBreak(9)
        })
        assertEquals(17, StaticPropertiesTest.fiberWithVerifyInstrumentationOn {
            continueBreak(100)
        })
    }

}
