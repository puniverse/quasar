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

public class FunTest {
    val scheduler = FiberForkJoinScheduler("test", 4, null, false)

    @Test fun testSimpleFun() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            @Suspendable override fun run(): Boolean {
                f()
                return true
            }
        }).start().get())
    }

    @Test fun testDefaultFunWithAllArgs() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            @Suspendable override fun run(): Boolean {
                fDef(true)
                return true
            }
        }).start().get())
    }

    @Test fun testDefaultFunWithoutSomeArgs() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            @Suspendable override fun run(): Boolean {
                // TODO https://youtrack.jetbrains.com/issue/KT-6930
                fDef()
                return true
            }
        }).start().get())
    }

    @Test fun testQuickFun() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            @Suspendable override fun run(): Boolean {
                fQuick()
                return true
            }
        }).start().get())
    }

    @Test fun testVarArgFun0() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            @Suspendable override fun run(): Boolean {
                fVarArg()
                return true
            }
        }).start().get())
    }

    @Test fun testVarArgFun1() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            @Suspendable override fun run(): Boolean {
                fVarArg(10)
                return true
            }
        }).start().get())
    }

    // TODO https://youtrack.jetbrains.com/issue/KT-6932

    @Test fun testFunRefInvoke() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            @Suspendable override fun run(): Boolean {

                (::fQuick)()
                return true
            }
        }).start().get())
    }

    @Test fun testFunRefArg() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            @Suspendable override fun run(): Boolean {
                seq(::fQuick, ::fQuick)()
                return true
            }
        }).start().get())
    }

    @Test fun testFunLambda() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            @Suspendable override fun run(): Boolean {
                (@Suspendable { _ : Int -> Fiber.sleep(10) })(1)
                return true
            }
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
