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
package co.paralleluniverse.kotlin.concurrent

import co.paralleluniverse.strands.concurrent.ReentrantReadWriteLock
import java.util.concurrent.CountDownLatch

/**
 * Executes the given [action] under the read lock of this lock.
 * @return the return value of the action.
 */
@Suppress("unused")
inline fun <T> ReentrantReadWriteLock.read(action: () -> T): T {
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
@Suppress("unused")
inline fun <T> ReentrantReadWriteLock.write(action: () -> T): T {
    val rl = readLock()

    val readCount = if (writeHoldCount == 0) readHoldCount else 0
    repeat(readCount) { rl.unlock() }

    val wl = writeLock()
    wl.lock()
    try {
        return action()
    } finally {
        repeat(readCount) { rl.lock() }
        wl.unlock()
    }
}

/**
 * Executes the given [operation] and await for CountDownLatch
 * @return the return value of the action.
 */
@Suppress("unused")
fun <T> Int.latch(operation: CountDownLatch.() -> T): T {
    val latch = CountDownLatch(this)
    val result = latch.operation()
    latch.await()
    return result
}
