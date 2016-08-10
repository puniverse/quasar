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
package co.paralleluniverse.kotlin

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.FiberScheduler
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.strands.SuspendableCallable
import co.paralleluniverse.strands.channels.*
import java.util.concurrent.TimeUnit

/**
 * @author circlespainter
 */
@Suspendable fun <T> fiber(start: Boolean, name: String?, scheduler: FiberScheduler?, stackSize: Int, block: () -> T): Fiber<T> {
    val sc = (SuspendableCallable<T> @Suspendable { block() })
    val ret =
        if (scheduler != null)
            Fiber(name, scheduler, stackSize, sc)
        else
            Fiber(name, stackSize, sc)
    if (start) ret.start()
    return ret
}
@Suspendable fun <T> fiber(block: () -> T): Fiber<T> =
    fiber(true, null, null, -1, block)
@Suspendable fun <T> fiber(start: Boolean, block: () -> T): Fiber<T> =
    fiber(start, null, null, -1, block)
@Suspendable fun <T> fiber(name: String, block: () -> T): Fiber<T> =
    fiber(true, name, null, -1, block)
@Suspendable fun <T> fiber(scheduler: FiberScheduler, block: () -> T): Fiber<T> =
    fiber(true, null, scheduler, -1, block)
@Suspendable fun <T> fiber(name: String, scheduler: FiberScheduler, block: () -> T): Fiber<T> =
    fiber(true, name, scheduler, -1, block)
@Suspendable fun <T> fiber(start: Boolean, scheduler: FiberScheduler, block: () -> T): Fiber<T> =
    fiber(start, null, scheduler, -1, block)
@Suspendable fun <T> fiber(start: Boolean, name: String, scheduler: FiberScheduler, block: () -> T): Fiber<T> =
    fiber(start, name, scheduler, -1, block)

open class SelectOp<out M>(private val wrappedSA: SelectAction<out M>) {
    fun getWrappedSelectAction(): SelectAction<out M> = wrappedSA
}
class Receive<M>(public val receivePort: ReceivePort<M>) : SelectOp<M>(Selector.receive(receivePort)) {
    @Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
    var msg: M? = null
        internal set(value) {
            field = value
        }
        get() = field
}
class Send<M>(val sendPort: SendPort<M>, val msg: M) : SelectOp<M>(Selector.send(sendPort, msg))

@Suspendable fun <R> select(actions: List<SelectOp<Any?>>, b: (SelectOp<Any?>?) -> R, priority: Boolean = false, timeout: Int = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): R {
    @Suppress("UNCHECKED_CAST")
    val sa = Selector.select(priority, timeout.toLong(), unit, actions.map{it.getWrappedSelectAction()}.toList() as List<SelectAction<Any?>>)
    if (sa != null) {
        val sOp: SelectOp<Any?> = actions[sa.index()]
        when (sOp) {
            is Receive<Any?> -> sOp.msg = sa.message()
        }
        return b(sOp)
    } else
        return b(null)
}
@Suspendable fun <R> select(vararg actions: SelectOp<Any?>, b: (SelectOp<Any?>?) -> R): R =   select(actions.toList(), b)
@Suspendable fun <R> select(timeout: Int, unit: TimeUnit, vararg actions: SelectOp<Any?>, b: (SelectOp<Any?>?) -> R): R =
    select(actions.toList(), b, false, timeout, unit)
@Suspendable fun <R> select(priority: Boolean, timeout: Int, unit: TimeUnit, vararg actions: SelectOp<Any?>, b: (SelectOp<Any?>?) -> R): R =
    select(actions.toList(), b, priority, timeout, unit)
