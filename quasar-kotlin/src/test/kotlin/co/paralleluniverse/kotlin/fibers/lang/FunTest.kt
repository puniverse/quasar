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
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.FiberForkJoinScheduler
import org.junit.Assert.assertTrue
import co.paralleluniverse.strands.SuspendableCallable
import org.junit.Test

/**
 * @author circlespainter
 */

fun seq(f: () -> Unit, g: () -> Unit): () -> Unit {
    println("seq")
    return (@Suspendable {f() ; g()})
}

@Suspendable fun f() {
    Fiber.sleep(10)
    @Suspendable fun f1() {
        Fiber.sleep(10)
    }
    f1()
}

@Suspendable fun fDef(@Suppress("UNUSED_PARAMETER") def: Boolean = true) {
    Fiber.sleep(10)
}

@Suspendable fun fQuick() {
    println("quick pre-sleep")
    Fiber.sleep(10)
    println("quick after-sleep")
}

@Suspendable fun fVarArg(vararg ls: Long) {
    for (l in ls) Fiber.sleep(l)
}

class FunTest {
    val scheduler = FiberForkJoinScheduler("test", 4, null, false)

    @Test fun testSimpleFun() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            f()
            true
        }).start().get())
    }

    @Test fun testDefaultFunWithAllArgs() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            fDef(true)
            true
        }).start().get())
    }

    @Test fun testDefaultFunWithoutSomeArgs() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            fDef()
            true
        }).start().get())
    }

    @Test fun testQuickFun() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            fQuick()
            true
        }).start().get())
    }

    @Test fun testVarArgFun0() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            fVarArg()
            true
        }).start().get())
    }

    @Test fun testVarArgFun1() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            fVarArg(10)
            true
        }).start().get())
    }

    @Test fun testFunRefInvoke() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            (::fQuick)()
            true
        }).start().get())
    }

    @Test fun testFunRefArg() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            seq(::fQuick, ::fQuick)()
            true
        }).start().get())
    }

    @Test fun testFunLambda() {
        assertTrue(Fiber(scheduler, SuspendableCallable<kotlin.Boolean> @Suspendable {
            (@Suspendable { ignored : Int -> Fiber.sleep(10) })(1)
            true
        }).start().get())
    }

    @Suspendable
    private fun callSusLambda(f: (Int) -> Unit, i: Int) =
            Fiber(scheduler, SuspendableCallable (@Suspendable {
                f(i)
                true
            })).start().get()

    @Test fun testFunLambda2() {
        assertTrue(callSusLambda(@Suspendable { Fiber.sleep(10) }, 1))
    }
}
