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
package co.paralleluniverse.kotlin.concurrent

import co.paralleluniverse.fibers.DefaultFiberScheduler
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.FiberScheduler
import co.paralleluniverse.strands.SuspendableCallable
import co.paralleluniverse.strands.concurrent.ReentrantReadWriteLock
import java.util.concurrent.CountDownLatch

/**
 * @author circlespainter
 */
public fun fiber<T>(block: () -> T, start: Boolean = false, name: String? = null, scheduler: FiberScheduler? = null, stackSize: Int = -1): Fiber<T> {
    val ret = Fiber(name, scheduler, stackSize, SuspendableCallable(block))
    if (start) ret.start()
    return ret
}

/**
 * Executes the given [action] under the read lock of this lock.
 * @return the return value of the action.
 */
public inline fun <T> ReentrantReadWriteLock.read(action: () -> T): T {
    val rl = readLock()
    rl.lock()
    try {
        return action()
    } finally {
        rl.unlock()
    }
}

/**
 * Executes the given [action] under the write lock of this lock.
 * The method does upgrade from read to write lock if needed
 * If such write has been initiated by checking some condition, the condition must be rechecked inside the action to avoid possible races
 * @return the return value of the action.
 */
public inline fun <T> ReentrantReadWriteLock.write(action: () -> T): T {
    val rl = readLock()

    val readCount = if (getWriteHoldCount() == 0) getReadHoldCount() else 0
    readCount times { rl.unlock() }

    val wl = writeLock()
    wl.lock()
    try {
        return action()
    } finally {
        readCount times { rl.lock() }
        wl.unlock()
    }
}

/**
 * Executes the given [action] and await for CountDownLatch
 * @return the return value of the action.
 */
public fun <T> Int.latch(operation: CountDownLatch.() -> T): T {
    val latch = CountDownLatch(this)
    val result = latch.operation()
    latch.await()
    return result
}
