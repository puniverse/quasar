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
Suspendable public inline fun fiber<T>(start: Boolean, name: String?, scheduler: FiberScheduler?, stackSize: Int, inlineOptions(InlineOption.ONLY_LOCAL_RETURN) block: () -> T): Fiber<T> {
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
Suspendable public inline fun  fiber<T>(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) block: () -> T): Fiber<T> =
    fiber(true, null, null, -1, block)
Suspendable public inline fun  fiber<T>(start: Boolean, inlineOptions(InlineOption.ONLY_LOCAL_RETURN) block: () -> T): Fiber<T> =
    fiber(start, null, null, -1, block)
Suspendable public inline fun  fiber<T>(name: String, inlineOptions(InlineOption.ONLY_LOCAL_RETURN) block: () -> T): Fiber<T> =
    fiber(true, name, null, -1, block)
Suspendable public inline fun  fiber<T>(scheduler: FiberScheduler, inlineOptions(InlineOption.ONLY_LOCAL_RETURN) block: () -> T): Fiber<T> =
    fiber(true, null, scheduler, -1, block)
Suspendable public inline fun  fiber<T>(name: String, scheduler: FiberScheduler, inlineOptions(InlineOption.ONLY_LOCAL_RETURN) block: () -> T): Fiber<T> =
    fiber(true, name, scheduler, -1, block)
Suspendable public inline fun  fiber<T>(start: Boolean, scheduler: FiberScheduler, inlineOptions(InlineOption.ONLY_LOCAL_RETURN) block: () -> T): Fiber<T> =
    fiber(start, null, scheduler, -1, block)
Suspendable public inline fun  fiber<T>(start: Boolean, name: String, scheduler: FiberScheduler, inlineOptions(InlineOption.ONLY_LOCAL_RETURN) block: () -> T): Fiber<T> =
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

Suspendable public inline fun select<R, M>(actions: List<SelectOp<M>>, b: (SelectOp<M>?) -> R, priority: Boolean = false, timeout: Int = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): R {
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
Suspendable public inline fun select<R, M>(vararg actions: SelectOp<M>, b: (SelectOp<M>?) -> R): R = select(actions.toList(), b)
Suspendable public inline fun select<R, M>(timeout: Int, unit: TimeUnit, vararg actions: SelectOp<M>, b: (SelectOp<M>?) -> R): R =
    select(actions.toList(), b, false, timeout, unit)
Suspendable public inline fun select<R, M>(priority: Boolean, timeout: Int, unit: TimeUnit, vararg actions: SelectOp<M>, b: (SelectOp<M>?) -> R): R =
    select(actions.toList(), b, priority, timeout, unit)

public data class On2<R, M, O : SelectOp<M>>(public val op: O, public val handler: (O) -> R)
@suppress("UNCHECKED_CAST")
Suspendable public inline fun select2<R, M>(actions: List<On2<R, M, *>>, elseHandler: () -> R, priority: Boolean = false, timeout: Int = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): R {
    val sa = Selector.select(priority, timeout.toLong(), unit, actions.map{it.op.getWrappedSelectAction() as SelectAction<M>}.toList())
    if (sa != null) {
        val sOp = actions.get(sa.index())
        when (sOp.op) {
            is Receive -> (sOp.op as Receive<M>).msg = sa.message()
        }
        return (sOp.handler as (SelectOp<M>) -> R)(sOp.op as (SelectOp<M>))
    } else
        return elseHandler()
}
@suppress("NOTHING_TO_INLINE")
@suppress("UNCHECKED_CAST")
Suspendable public inline fun select2<R, M>(vararg actions: On2<R, M, *>): R =
    select2(actions.toList(), /* Fake, never used */ { (actions.get(0).handler as (SelectOp<M>) -> R)(actions.get(0).op as SelectOp<M>) })
Suspendable public inline fun select2<R, M>(timeout: Int, unit: TimeUnit, elseHandler: () -> R, vararg actions: On2<R, M, *>): R =
    select2(actions.toList(), elseHandler, false, timeout, unit)
Suspendable public inline fun select2<R, M>(priority: Boolean, timeout: Int, unit: TimeUnit, elseHandler: () -> R, vararg actions: On2<R, M, *>): R =
    select2(actions.toList(), elseHandler, priority, timeout, unit)

public data class On3<R, M>(public val op: SelectOp<M>, public val handler: (SelectOp<M>) -> R)
Suspendable public inline fun select3<R, M>(actions: List<On3<R, M>>, elseHandler: () -> R, priority: Boolean = false, timeout: Int = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): R {
    val sa = Selector.select(priority, timeout.toLong(), unit, actions.map{it.op.getWrappedSelectAction() as SelectAction<M>}.toList())
    if (sa != null) {
        val sOp = actions.get(sa.index())
        when (sOp.op) {
            is Receive -> sOp.op.msg = sa.message()
        }
        return sOp.handler(sOp.op)
    } else
        return elseHandler()
}
@suppress("NOTHING_TO_INLINE")
Suspendable public inline fun select3<R, M>(vararg actions: On3<R, M>): R =
        select3(actions.toList(), /* Fake, never used */ { actions.get(0).handler(actions.get(0).op) })
Suspendable public inline fun select3<R, M>(timeout: Int, unit: TimeUnit, elseHandler: () -> R, vararg actions: On3<R, M>): R =
        select3(actions.toList(), elseHandler, false, timeout, unit)
Suspendable public inline fun select3<R, M>(priority: Boolean, timeout: Int, unit: TimeUnit, elseHandler: () -> R, vararg actions: On3<R, M>): R =
        select3(actions.toList(), elseHandler, priority, timeout, unit)
