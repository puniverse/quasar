package co.paralleluniverse.kotlin.fibers

import co.paralleluniverse.common.util.SystemProperties
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import org.junit.Assume.assumeTrue

object StaticPropertiesTest {

    const val verifyInstrumentationKey = "co.paralleluniverse.fibers.verifyInstrumentation"

    fun <T> withVerifyInstrumentationOn(statement: () -> T): T {
        return withKey(verifyInstrumentationKey, statement)
    }

    fun <T> withKey(key : String, statement: () -> T): T{
        val originalValue = System.getProperty(key)
        System.setProperty(key, "true")
        Fiber.readSystemProperties()
        try {
            return statement()
        } finally {
            if (originalValue == null) {
                System.clearProperty(key)
            } else {
                System.setProperty(key, originalValue)
            }
            Fiber.readSystemProperties()
        }
    }

    fun <T> fiberWithVerifyInstrumentationOn(f: () -> T) : T {
        return withVerifyInstrumentationOn {
            assumeTrue(SystemProperties.isEmptyOrTrue(verifyInstrumentationKey))
            val fiber = object : Fiber<T>() {
                @Suspendable
                override fun run(): T {
                    return f()
                }
            }
            fiber.start().get()
        }
    }
}
