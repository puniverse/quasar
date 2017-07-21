/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015-2017, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.strands.channels.ReceivePort
import co.paralleluniverse.strands.channels.SelectAction
import co.paralleluniverse.strands.channels.Selector
import co.paralleluniverse.strands.channels.SendPort
import co.paralleluniverse.strands.concurrent.ReentrantLock
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock

/**
 * @author circlespainter
 */
@Suspendable fun <T> fiber(start: Boolean = true, name: String? = null, scheduler: FiberScheduler? = null, stackSize: Int = -1, block: () -> T): Fiber<T> {
    val sc = (SuspendableCallable<T> @Suspendable { block() })
    val ret =
        if (scheduler != null)
            Fiber(name, scheduler, stackSize, sc)
        else
            Fiber(name, stackSize, sc)
    if (start) ret.start()
    return ret
}

open class SelectOp<out M>(private val wrappedSA: SelectAction<out M>) {
    fun getWrappedSelectAction(): SelectAction<out M> = wrappedSA
}
class Receive<M>(receivePort: ReceivePort<M>) : SelectOp<M>(Selector.receive(receivePort)) {
    @Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
    var msg: M? = null
        internal set(value) {
            field = value
        }
        get() = field
}
class Send<out M>(sendPort: SendPort<M>, msg: M) : SelectOp<M>(Selector.send(sendPort, msg))

@Suspendable fun <R> select(actions: List<SelectOp<Any?>>, b: (SelectOp<Any?>?) -> R, priority: Boolean = false, timeout: Int = -1, unit: TimeUnit = TimeUnit.MILLISECONDS): R {
    @Suppress("UNCHECKED_CAST")
    val sa = Selector.select(priority, timeout.toLong(), unit, actions.map{it.getWrappedSelectAction()} as List<SelectAction<Any?>>)
    if (sa != null) {
        val sOp: SelectOp<Any?> = actions[sa.index()]
        when (sOp) {
            is Receive<Any?> -> sOp.msg = sa.message()
        }
        return b(sOp)
    } else
        return b(null)
}
// TODO With Java 7 compiler (unsupported as of Kotlin 1.1) the bytecode seemingly references the wrong method w.r.t. the ones found by instrumentation!
@Suspendable fun <R> select(vararg actions: SelectOp<Any?>, b: (SelectOp<Any?>?) -> R): R = select(actions.toList(), b)
@Suspendable fun <R> select(timeout: Int, unit: TimeUnit, vararg actions: SelectOp<Any?>, b: (SelectOp<Any?>?) -> R): R =
    select(actions.toList(), b, false, timeout, unit)
@Suppress("unused") @Suspendable fun <R> select(priority: Boolean, timeout: Int, unit: TimeUnit, vararg actions: SelectOp<Any?>, b: (SelectOp<Any?>?) -> R): R =
    select(actions.toList(), b, priority, timeout, unit)


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// The following is adapted from https://github.com/JetBrains/kotlin/blob/v1.1.3/libraries/stdlib/src/kotlin/util/Lazy.kt#L108
// See the NOTICE file
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private object UNINITIALIZED_VALUE

private inline fun <R> withLock(lock: Lock, block: () -> R): R {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")

    lock.lock()
    try {
        return block()
    }
    finally {
        lock.unlock()
    }
}

private class InitializedLazyImpl<out T>(override val value: T) : Lazy<T>, java.io.Serializable {
    override fun isInitialized(): Boolean = true
    override fun toString(): String = value.toString()
}

private class FiberLockedLazyImpl<out T>(initializer: () -> T, lock: Lock? = null) : Lazy<T>, java.io.Serializable {
    private var initializer: (() -> T)? = initializer
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
    // final field is required to enable safe publication of constructed instance
    private val lock = lock ?: ReentrantLock()

    override val value: T
        @Suspendable get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return _v1 as T
            }

            return withLock(lock) {
                val _v2 = _value
                if (_v2 !== UNINITIALIZED_VALUE) {
                    @Suppress("UNCHECKED_CAST") (_v2 as T)
                }
                else {
                    val typedValue = initializer!!()
                    _value = typedValue
                    initializer = null
                    typedValue
                }
            }
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedLazyImpl(value)
}

fun <T> quasarLazy(initializer: () -> T): Lazy<T> = FiberLockedLazyImpl(initializer)
