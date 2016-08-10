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

import org.junit.Test
import org.junit.Assert.*
import co.paralleluniverse.fibers.FiberForkJoinScheduler
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.strands.SuspendableRunnable
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.strands.channels.Channels
import java.util.concurrent.TimeUnit

/**
 *
 * @author circlespainter
 */
class ControlFlowTest {
    private val scheduler = FiberForkJoinScheduler("test", 4, null, false);

    @Test
    fun testForAndWhile() {
        val ch = Channels.newIntChannel(0);
        val vals = listOf(0, 1)
        val fiberSend = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            for(v in vals)
                ch.send(v)
            ch.close()
        }).start()
        val fiberReceive = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            var l = listOf<Int>()
            var i = ch.receive()
            while(i != null) {
                l = l.plus(i)
                i = ch.receive()
            }
            assertEquals(vals, l)
        }).start()
        fiberSend.join()
        fiberReceive.join()
    }

    @Test
    fun testWhen() {
        val ch = Channels.newIntChannel(0);
        val vals = listOf(1, 101)
        val fiberSend = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            for(v in vals)
                ch.send(v)
            ch.close()
        }).start()
        val fiberReceive = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            var l = listOf<Boolean>()
            var i = ch.receive()
            while(i != null) {
                when (i) {
                    in 1..100 -> {
                        l = l.plus(true)
                        i = ch.receive()
                    }
                    else -> {
                        l = l.plus(false)
                        i = ch.receive()
                    }
                }
            }
            assertEquals(listOf(true, false), l)
        }).start()
        fiberSend.join()
        fiberReceive.join()
    }

    @Test
    fun testHOFun() {
        val ch = Channels.newIntChannel(0);
        val vals = listOf(0, 1)
        val fiberSend = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            vals.forEach { ch.send(it) }
            ch.close()
        }).start()
        val fiberReceive = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            var l = listOf<Int>()
            var i = ch.receive()
            while(i != null) {
                l = l.plus(i)
                i = ch.receive()
            }
            @Suspendable fun f() {
                l.forEach {
                    x ->
                    Fiber.park(10, TimeUnit.MILLISECONDS)
                    if (x % 2 != 0)
                        return@f
                }
            }
            f()
            assertEquals(vals, l)
        }).start()
        fiberSend.join()
        fiberReceive.join()
    }
}