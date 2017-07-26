package co.paralleluniverse.kotlin

import co.paralleluniverse.fibers.SuspendExecution
import co.paralleluniverse.fibers.Suspendable
import java.io.Serializable

@FunctionalInterface
interface KotlinSamInterface : Serializable {
    @Suspendable
    @Throws(SuspendExecution::class)
    fun doSomething()
}

@Suspendable
@Throws(SuspendExecution::class)
fun accept(function: KotlinSamInterface) {
    function.doSomething()
}