/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.fibers.SuspendExecution
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.strands.SuspendableCallable
import co.paralleluniverse.strands.channels.*
import java.util.concurrent.TimeUnit

/**
 * @author circlespainter
 */
Suspendable public fun fiber<T>(start: Boolean, name: String?, scheduler: FiberScheduler?, stackSize: Int, block: () -> T): Fiber<T> {
    val sc = @Suspendable object : SuspendableCallable<T> {
        @throws(SuspendExecution::class) Suspendable override fun run(): T = block()
    }
    val ret =
        if (scheduler != null)
            Fiber(name, scheduler, stackSize, sc)
        else
            Fiber(name, stackSize, sc)
    if (start) ret.start()
    return ret
}
Suspendable public fun  fiber<T>(block: () -> T): Fiber<T> =
    fiber(true, null, null, -1, block)
Suspendable public fun  fiber<T>(start: Boolean, block: () -> T): Fiber<T> =
    fiber(start, null, null, -1, block)
Suspendable public fun  fiber<T>(name: String, block: () -> T): Fiber<T> =
    fiber(true, name, null, -1, block)
Suspendable public fun  fiber<T>(scheduler: FiberScheduler, block: () -> T): Fiber<T> =
    fiber(true, null, scheduler, -1, block)
Suspendable public fun  fiber<T>(name: String, scheduler: FiberScheduler, block: () -> T): Fiber<T> =
    fiber(true, name, scheduler, -1, block)
Suspendable public fun  fiber<T>(start: Boolean, scheduler: FiberScheduler, block: () -> T): Fiber<T> =
    fiber(start, null, scheduler, -1, block)
Suspendable public fun  fiber<T>(start: Boolean, name: String, scheduler: FiberScheduler, block: () -> T): Fiber<T> =
    fiber(start, name, scheduler, -1, block)

public abstract data class SelectOp<M>(private val wrappedSA: SelectAction<M>) {
    public fun getWrappedSelectAction(): SelectAction<M> = wrappedSA
}
public data class Receive<M>(public val receivePort: ReceivePort<M>) : SelectOp<M>(Selector.receive(receivePort)) {
    public var msg: M = null
        internal set(value) {
            $msg = value
        }
        get() = $msg
}
public data class Send<M>(public val sendPort: SendPort<M>, public val msg: M) : SelectOp<M>(Selector.send(sendPort, msg))

Suspendable public fun select<R, M>(actions: List<SelectOp<M>>, b: (SelectOp<M>?) -> R, priority: Boolean = false, timeout: Int = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): R {
    val sa = Selector.select(priority, timeout.toLong(), unit, actions.map{it.getWrappedSelectAction()}.toList())
    if (sa != null) {
        val sOp = actions.get(sa.index())
        when (sOp) {
            is Receive -> sOp.msg = sa.message()
        }
        return b(sOp)
    } else
        return b(null)
}
Suspendable public fun select<R, M>(vararg actions: SelectOp<M>, b: (SelectOp<M>?) -> R): R = select(actions.toList(), b)
Suspendable public fun select<R, M>(timeout: Int, unit: TimeUnit, vararg actions: SelectOp<M>, b: (SelectOp<M>?) -> R): R =
    select(actions.toList(), b, false, timeout, unit)
Suspendable public fun select<R, M>(priority: Boolean, timeout: Int, unit: TimeUnit, vararg actions: SelectOp<M>, b: (SelectOp<M>?) -> R): R =
    select(actions.toList(), b, priority, timeout, unit)
