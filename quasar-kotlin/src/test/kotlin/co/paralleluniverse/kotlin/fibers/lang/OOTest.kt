/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.kotlin.fibers.lang

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.FiberForkJoinScheduler
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.strands.SuspendableCallable
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KProperty

/**
 * @author circlespainter
 */
class OOTest {
    val scheduler = FiberForkJoinScheduler("test", 4, null, false)
    val iv = 1
        @Suspendable get() { Fiber.sleep(1) ; return field }
    var mv = 1
        @Suspendable get() { Fiber.sleep(1) ; return field }
        @Suspendable set(v) { Fiber.sleep(1) ; field = v }
    var md by D()
        @Suspendable get
        @Suspendable set

    class D {
        @Suspendable operator fun getValue(thisRef: Any?, prop: KProperty<*>): String {
            Fiber.sleep(1)
            return "$thisRef, thank you for delegating '${prop.name}' to me!"
        }
        @Suppress("UNUSED_PARAMETER")
        @Suspendable operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: String) = Fiber.sleep(1)
    }

    @Suspendable fun outerDoSleep() = Fiber.sleep(2)

    open class Base (val data: Int = 0) {
        // NOT SUPPORTED: Kotlin's initializers are named <init> and we don't instrument those. Not an issue
        // because they're called by constructors which we don't instrument either (because it most probably
        // impossible to unpark them.

        // [Suspendable] { Fiber.sleep(1) }

        open @Suspendable fun doSleep() {
            data
            Fiber.sleep(10)
        }
    }

    interface BaseTrait1 {
        @Suspendable fun doSleep() {
            Fiber.sleep(5)
        }
    }

    interface BaseTrait2 {
        @Suspendable fun doSleep() {
            Fiber.sleep(7)
        }
    }

    open class Derived(data: Int) : Base(data) {
        final override @Suspendable fun doSleep() {
            Fiber.sleep(20)
        }
    }

    abstract class DerivedAbstract1 : Base(1) {
        override abstract @Suspendable fun doSleep()
    }

    class DerivedDerived1 : Base(1) {
        override @Suspendable fun doSleep() {
            Fiber.sleep(30)
        }
    }

    abstract class DerivedDerived2 : BaseTrait1, DerivedAbstract1()

    class DerivedDerived3 : DerivedAbstract1(), BaseTrait1, BaseTrait2 {
        override @Suspendable fun doSleep() {
            super<BaseTrait2>.doSleep()
        }
    }

    open inner class InnerDerived : DerivedAbstract1(), BaseTrait2 {
        override @Suspendable fun doSleep() {
            outerDoSleep()
        }
    }

    // TODO: https://youtrack.jetbrains.com/issue/KT-10532
    /*
    companion object : DerivedDerived2(), BaseTrait1 {
        override @Suspendable fun doSleep() {
            super<DerivedDerived2>.doSleep()
        }
    }

    @Test public fun testOODefObject() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            OOTest.Companion.doSleep()
            true
        }).start().get())
    }
    */

    class Data : DerivedDerived2(), BaseTrait2 {
        @Suspendable override fun doSleep() {
            super<BaseTrait2>.doSleep()
        }

        @Suspendable fun doSomething() {
            Fiber.sleep(10)
        }
    }

    class Delegating(bb2: BaseTrait2) : Base(), BaseTrait2 by bb2 {
        @Suspendable override fun doSleep() {
            Fiber.sleep(8)
        }
    }

    enum class E(val data: Int?) {
        V1(0),
        V2(1) {
            @Suspendable override fun enumFun() {
                Fiber.sleep(10)
            }
        };

        @Suspendable open fun enumFun() {
            data
            Fiber.sleep(10)
        }

    }

    @Suppress("unused")
    @Suspendable fun Any?.doFiberSleep() {
        Fiber.sleep(10)
    }

    @Suppress("unused")
    var E.mvE: Int
        @Suspendable get() { Fiber.sleep(1) ; return 1 }
        @Suspendable set(v) { Fiber.sleep(1) }
    @Suppress("unused")
    var E.mdE by D()
        @Suspendable get
        @Suspendable set

    object O : DerivedDerived2()

    @Test fun testOOSimple() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            Base().doSleep()
            true
        }).start().get())
    }

    @Test fun testOOInheritingObjectLiteral() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            (object : BaseTrait1 {}).doSleep()
            true
        }).start().get())
    }

    @Test fun testDerived() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            Derived(1).doSleep()
            true
        }).start().get())
    }

    @Test fun testOOOverridingObjectLiteral() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            (object : DerivedAbstract1() {
                override @Suspendable fun doSleep() {
                    Fiber.sleep(1)
                }
            }).doSleep()
            true
        }).start().get())
    }

    @Test fun testOODerived1() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            DerivedDerived1().doSleep()
            true
        }).start().get())
    }

    @Test fun testOODerived2() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            (object : DerivedDerived2() {}).doSleep()
            true
        }).start().get())
    }

    @Test fun testOODerived3() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            DerivedDerived3().doSleep()
            true
        }).start().get())
    }

    @Test fun testOOInnerDerived() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            InnerDerived().doSleep()
            true
        }).start().get())
    }

    @Test fun testOOOuter() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            outerDoSleep()
            true
        }).start().get())
    }

    @Test fun testOOData() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            Data().doSomething()
            true
        }).start().get())
    }

    @Test fun testOODataInherited() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            Data().doSleep()
            true
        }).start().get())
    }

    @Test fun testOOEnum1() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            E.V1.enumFun()
            true
        }).start().get())
    }

    @Test fun testOOEnum2() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            E.V2.enumFun()
            true
        }).start().get())
    }

    @Test fun testOOEnumExt() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            E.V1.doFiberSleep()
            true
        }).start().get())
    }

    @Test fun testOOValPropGet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            iv
            true
        }).start().get())
    }

    @Test fun testOOVarPropGet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            mv
            true
        }).start().get())
    }

    @Test fun testOOVarPropSet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            mv = 3
            true
        }).start().get())
    }

    @Test fun testOOExtVarPropGet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            E.V2.mvE
            true
        }).start().get())
    }

    @Test fun testOOExtVarPropSet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            E.V2.mvE = 4
            true
        }).start().get())
    }

    @Test fun testOOObjectDecl() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            O.doSleep()
            true
        }).start().get())
    }

    @Test fun testOODeleg() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            Delegating(DerivedDerived3()).doSleep()
            true
        }).start().get())
    }

    @Test fun testOOValPropRefGet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            OOTest::iv.get(this@OOTest)
            true
        }).start().get())
    }

    @Test fun testOOVarPropRefGet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            OOTest::mv.get(this@OOTest)
            true
        }).start().get())
    }

    @Test fun testOOVarPropRefSet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            OOTest::mv.set(this@OOTest, 3)
            true
        }).start().get())
    }

    @Test fun testOODelegVarPropRefGet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            OOTest::md.get(this@OOTest)
            true
        }).start().get())
    }

    @Test fun testOODelegVarPropRefSet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            OOTest::md.set(this@OOTest, "hi")
            true
        }).start().get())
    }

    @Test fun testOODelegVarPropGet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            md
            true
        }).start().get())
    }

    @Test fun testOODelegVarPropSet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            md = "hi"
            true
        }).start().get())
    }

    @Test fun testOOExtDelegVarPropSet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            E.V1.mdE = ""
            true
        }).start().get())
    }

    @Test fun testOOExtDelegVarPropGet() {
        assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            E.V1.mdE
            true
        }).start().get())
    }
}
