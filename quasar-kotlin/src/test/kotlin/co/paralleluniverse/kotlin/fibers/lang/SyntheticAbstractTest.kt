package co.paralleluniverse.kotlin.fibers.lang

import co.paralleluniverse.common.util.SystemProperties
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.fibers.StaticPropertiesTest
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

abstract class SyntheticAbstractClass  {

    // Complex template types.
    @Suspendable
    fun complex(m: MyMap, b: Boolean=true): MyMap {
        Fiber.yield()
        return if (b) {
            m
        } else {
            mapOf<Int, Int>(6 to 7)
        }
    }

    @Suspendable
    abstract fun complexabs(m: MyMap, b: Boolean=false): MyMap

    // Primitve types.
    @Suspendable
    fun primitive(a: Int, b: Int=6): Int {
        Fiber.yield()
        return a + b
    }

    @Suspendable
    abstract fun primitiveabs(a: Int, b: Int=3): Int
}

class SyntheticConcreteClass : SyntheticAbstractClass() {
    @Suspendable
    override fun complexabs(m: MyMap, b: Boolean): MyMap {
        Fiber.yield()
        return if (b) {
            m
        } else {
            mapOf<Int, Int>(1 to 1)
        }
    }

    // Does this have to be marked suspendable when overriding ?
    @Suspendable
    override fun primitiveabs(a: Int, b: Int): Int {
        Fiber.yield()
        return a - b
    }
}

class SyntheticAbstractTest {

    @Test fun `abstract primitive`() {

        StaticPropertiesTest.withVerifyInstrumentationOn {

            assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<Int>() {
                val ac : SyntheticAbstractClass = SyntheticConcreteClass()
                @Suspendable
                override fun run(): Int {
                    return ac.primitive(4)
                }
            }

            val actual = fiber.start().get()
            assertEquals(10, actual)
        }
    }

    @Test fun `abstract primitive abs`() {

        StaticPropertiesTest.withVerifyInstrumentationOn {

            assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<Int>() {
                val ac : SyntheticAbstractClass = SyntheticConcreteClass()
                @Suspendable
                override fun run(): Int {
                    return ac.primitiveabs(6)
                }
            }

            val actual = fiber.start().get()
            assertEquals(3, actual)
        }
    }

    @Test fun `abstract complex`() {

        StaticPropertiesTest.withVerifyInstrumentationOn {

            assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<MyMap>() {
                val ac : SyntheticAbstractClass = SyntheticConcreteClass()
                @Suspendable
                override fun run(): MyMap {
                    return ac.complex(mapOf(1 to 4, 5 to 5))
                }
            }

            val actual = fiber.start().get()
            assertEquals(mapOf(1 to 4, 5 to 5), actual)
        }
    }

    @Test fun `abstract complex abs`() {

        StaticPropertiesTest.withVerifyInstrumentationOn {

            assumeTrue(SystemProperties.isEmptyOrTrue(StaticPropertiesTest.verifyInstrumentationKey))

            val fiber = object : Fiber<MyMap>() {
                val ac : SyntheticAbstractClass = SyntheticConcreteClass()
                @Suspendable
                override fun run(): MyMap {
                    return ac.complexabs(mapOf(1 to 11))
                }
            }

            val actual = fiber.start().get()
            assertEquals(mapOf(1 to 1), actual)
        }
    }

}
