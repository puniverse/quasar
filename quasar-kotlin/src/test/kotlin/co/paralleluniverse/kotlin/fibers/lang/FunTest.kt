package co.paralleluniverse.kotlin.fibers.lang

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.FiberForkJoinScheduler
import org.junit.Assert.assertTrue
import co.paralleluniverse.strands.SuspendableCallable
import org.junit.Test

/**
 * @author circlespainter.
 */

fun seq(f: () -> Unit, g: () -> Unit): () -> Unit {
    return {f() ; g()}
}

Suspendable fun f() {
    Fiber.sleep(10)
    [Suspendable] fun f1() {
        Fiber.sleep(10)
    }
    f1()
}

Suspendable fun fDef(def: Boolean = true) {
    Fiber.sleep(10)
}

Suspendable fun fQuick() = Fiber.sleep(10)

Suspendable fun fVarArg(vararg ls: Long) {
    for (l in ls) Fiber.sleep(l)
}

public class FunTest {
    val scheduler = FiberForkJoinScheduler("test", 4, null, false)

    Test fun testSimpleFun() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                f()
                return true
            }
        }).start().get())
    }

    Test fun testDefaultFunWithAllArgs() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                fDef(true)
                return true
            }
        }).start().get())
    }

    Test fun testDefaultFunWithoutSomeArgs() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                // TODO https://youtrack.jetbrains.com/issue/KT-6930
                fDef()
                return true
            }
        }).start().get())
    }

    Test fun testQuickFun() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                fQuick()
                return true
            }
        }).start().get())
    }

    Test fun testVarArgFun0() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                fVarArg()
                return true
            }
        }).start().get())
    }

    Test fun testVarArgFun1() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                fVarArg(10)
                return true
            }
        }).start().get())
    }

    // TODO https://youtrack.jetbrains.com/issue/KT-6932

    Test fun testFunRefInvoke() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {

                (::fQuick)()
                return true
            }
        }).start().get())
    }

    Test fun testFunRefArg() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                seq(::fQuick, ::fQuick)()
                return true
            }
        }).start().get())
    }

    Test fun testFunLambda() {
        assertTrue(Fiber(scheduler, object : SuspendableCallable<Boolean> {
            Suspendable override fun run(): Boolean {
                ([Suspendable] {(a : Int): Unit -> Fiber.sleep(10) })(1)
                return true
            }
        }).start().get())
    }
}
