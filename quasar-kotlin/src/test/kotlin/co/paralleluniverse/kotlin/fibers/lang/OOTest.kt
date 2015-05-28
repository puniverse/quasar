/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.hamcrest.CoreMatchers.*
import org.junit.Ignore
import co.paralleluniverse.strands.SuspendableRunnable
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.FiberForkJoinScheduler
import co.paralleluniverse.strands.channels.Channels
import co.paralleluniverse.fibers.Fiber
import java.util.concurrent.TimeUnit
import co.paralleluniverse.strands.SuspendableCallable

/**
 *
 * @author circlespainter
 */
public class OOTest {
    val scheduler = FiberForkJoinScheduler("test", 4, null, false)
    val iv = 1
        @Suspendable get() { Fiber.sleep(1) ; return $iv }
    var mv = 1
        @Suspendable get() { Fiber.sleep(1) ; return $mv }
        @Suspendable set(v) { Fiber.sleep(1) ; $mv = v }
    var md by D()
        @Suspendable get
        @Suspendable set

    class D {
        Suspendable fun get(thisRef: Any?, prop: PropertyMetadata): String {
            Fiber.sleep(1)
            return "$thisRef, thank you for delegating '${prop.name}' to me!"
        }
        @suppress("UNUSED_PARAMETER")
        Suspendable fun set(thisRef: Any?, prop: PropertyMetadata, value: String) {
            Fiber.sleep(1)
        }
    }

    Suspendable fun outerDoSleep() {
        Fiber.sleep(2)
    }

    open class Base (val data: Int = 0) {
        // NOT SUPPORTED: Kotlin's initializers are named <init> and we don't instrument those. Not an issue
        // because they're called by constructors which we don't instrument either (because it most probably
        // impossible to unpark them.

        // [Suspendable] { Fiber.sleep(1) }

        open Suspendable fun doSleep() {
            data
            Fiber.sleep(10)
        }
    }

    interface BaseTrait1 {
        Suspendable fun doSleep() {
            Fiber.sleep(5)
        }
    }

    interface BaseTrait2 {
        Suspendable fun doSleep() {
            Fiber.sleep(7)
        }
    }

    open class Derived(data: Int) : Base(data) {
        final override Suspendable fun doSleep() {
            Fiber.sleep(20)
        }
    }

    abstract class DerivedAbstract1 : Base(1) {
        override abstract Suspendable fun doSleep()
    }

    class DerivedDerived1 : Base(1) {
        override Suspendable fun doSleep() {
            Fiber.sleep(30)
        }
    }

    open class DerivedDerived2 : DerivedAbstract1(), BaseTrait1

    class DerivedDerived3 : DerivedAbstract1(), BaseTrait1, BaseTrait2 {
        override Suspendable fun doSleep() {
            super<BaseTrait2>.doSleep()
        }
    }

    open inner class InnerDerived : DerivedAbstract1(), BaseTrait2 {
        override Suspendable fun doSleep() {
            outerDoSleep()
        }
    }

    companion object : DerivedDerived2(), BaseTrait1 {
        override Suspendable fun doSleep() {
            super<DerivedDerived2>.doSleep()
        }
    }

    data class Data() : DerivedDerived2(), BaseTrait2 {
        Suspendable override fun doSleep() {
            super<BaseTrait2>.doSleep()
        }

        Suspendable public fun doSomething() {
            Fiber.sleep(10)
        }
    }

    class Delegating(bb2: BaseTrait2) : Base(), BaseTrait2 by bb2

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

    Suspendable fun Any?.doFiberSleep() {
        Fiber.sleep(10)
    }

    var E.mvE: Int
        @Suspendable get() { Fiber.sleep(1) ; return 1 }
        @Suspendable set(v) { Fiber.sleep(1) }
    var E.mdE by D()
        @Suspendable get
        @Suspendable set

    object O : DerivedDerived2()

    Test public fun testOOSimple() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                Base().doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOOInheritingObjectLiteral() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                (object : BaseTrait1 {}).doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testDerived() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                Derived(1).doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOOOverridingObjectLiteral() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                (object : DerivedAbstract1() {
                    override Suspendable fun doSleep() {
                        Fiber.sleep(1)
                    }
                }).doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOODerived1() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                DerivedDerived1().doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOODerived2() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                DerivedDerived2().doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOODerived3() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                DerivedDerived3().doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOOInnerDerived() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                InnerDerived().doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOOOuter() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                outerDoSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOODefObject() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                OOTest.Companion.doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOOData() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                Data().doSomething()
                return true
            }
        }).start().get())
    }

    Test public fun testOODataInherited() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                Data().doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOOEnum1() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                E.V1.enumFun()
                return true
            }
        }).start().get())
    }

    Test public fun testOOEnum2() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                E.V2.enumFun()
                return true
            }
        }).start().get())
    }

    Test public fun testOOEnumExt() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                E.V1.doFiberSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOOValPropGet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                iv
                return true
            }
        }).start().get())
    }

    Test public fun testOOVarPropGet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                mv
                return true
            }
        }).start().get())
    }

    Test public fun testOOVarPropSet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                mv = 3
                return true
            }
        }).start().get())
    }

    Test public fun testOOExtVarPropGet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                E.V2.mvE
                return true
            }
        }).start().get())
    }

    Test public fun testOOExtVarPropSet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                E.V2.mvE = 4
                return true
            }
        }).start().get())
    }

    Test public fun testOOObjectDecl() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                O.doSleep()
                return true
            }
        }).start().get())
    }

    Test public fun testOODeleg() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                Delegating(DerivedDerived3()).doSleep()
                return true
            }
        }).start().get())
    }

    // TODO these require instrumenting the language's Runtime, not best for performance nor to stay decoupled from the language runtime; also for some reason they run very slow and get the tests stuck for quite some time.

    Test public fun testOOValPropRefGet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                ::iv.get(this@OOTest)
                return true
            }
        }).start().get())
    }

    Test public fun testOOVarPropRefGet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                ::mv.get(this@OOTest)
                return true
            }
        }).start().get())
    }

    Test public fun testOOVarPropRefSet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                ::mv.set(this@OOTest, 3)
                return true
            }
        }).start().get())
    }

    Test public fun testOODelegVarPropRefGet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                ::md.get(this@OOTest)
                return true
            }
        }).start().get())
    }

    Test public fun testOODelegVarPropRefSet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                ::md.set(this@OOTest, "hi")
                return true
            }
        }).start().get())
    }

    Test public fun testOODelegVarPropGet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                md
                return true
            }
        }).start().get())
    }

    Test public fun testOODelegVarPropSet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                md = "hi"
                return true
            }
        }).start().get())
    }

    Test public fun testOOExtDelegVarPropSet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                E.V1.mdE = ""
                return true
            }
        }).start().get())
    }

    Test public fun testOOExtDelegVarPropGet() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                E.V1.mdE
                return true
            }
        }).start().get())
    }
}
