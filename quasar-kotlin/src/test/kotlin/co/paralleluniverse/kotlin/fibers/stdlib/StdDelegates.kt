/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2017, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.kotlin.fibers.stdlib

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.FiberForkJoinScheduler
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.quasarLazy
import co.paralleluniverse.strands.SuspendableCallable
import org.junit.Assert
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.test.assertNull

/**
 * @author circlespainter
 */
class StdDelegates {
    val scheduler = FiberForkJoinScheduler("test", 4, null, false)

    @Test fun testLocalValLazySyncDelegProp() {
        val ipLazySync by quasarLazy @Suspendable {
            Fiber.sleep(1)
            true
        }
        Assert.assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            ipLazySync
        }).start().get())
    }

    private @get:Suspendable val ipLazySync by quasarLazy @Suspendable {
        Fiber.sleep(1)
        true
    }

    @Test fun testValLazySyncDelegProp() {
        Assert.assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            ipLazySync
        }).start().get())
    }

    @Test fun testLocalValLazyPubDelegProp() {
        val ipLazyPub by lazy(LazyThreadSafetyMode.PUBLICATION) @Suspendable {
            Fiber.sleep(1)
            true
        }
        Assert.assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            ipLazyPub
        }).start().get())
    }

    private @get:Suspendable val ipLazyPub by lazy(LazyThreadSafetyMode.PUBLICATION) @Suspendable {
        Fiber.sleep(1)
        true
    }

    @Test fun testValLazyPubDelegProp() {
        Assert.assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            ipLazyPub
        }).start().get())
    }

    @Test fun testLocalValLazyUnsafeDelegProp() {
        val ipLazyNone by lazy(LazyThreadSafetyMode.NONE) @Suspendable {
            Fiber.sleep(1)
            true
        }
        Assert.assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            ipLazyNone
        }).start().get())
    }

    private @get:Suspendable val ipLazyNone by lazy(LazyThreadSafetyMode.NONE) @Suspendable {
        Fiber.sleep(1)
        true
    }

    @Test fun testValLazyUnsafeDelegProp() {
        Assert.assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            ipLazyNone
        }).start().get())
    }

    @Test fun testLocalValObservableDelegProp() {
        val ivObs: Boolean? by Delegates.observable(true) {
            _, old, new ->
            Fiber.sleep(1)
            println("$old -> $new")
        }
        Assert.assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            ivObs
        }).start().get())
    }

    @get:Suspendable val ivObs: Boolean? by Delegates.observable(true) {
        _, old, new ->
        Fiber.sleep(1)
        println("$old -> $new")
    }

    @Test fun testValObservableDelegProp() {
        Assert.assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            ivObs
        }).start().get())
    }

    @Test fun testLocalVarObservableGetDelegProp() {
        @Suppress("CanBeVal")
        var mvObs: Boolean? by Delegates.observable<Boolean?>(null) {
            _, old, new ->
            Fiber.sleep(1)
            println("$old -> $new")
        }
        assertNull(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            mvObs
        }).start().get())
    }

    @Test fun testLocalVarObservableSetDelegProp() {
        var mvObs: Boolean? by Delegates.observable<Boolean?>(null) {
            _, old, new ->
            Fiber.sleep(1)
            println("$old -> $new")
        }
        Assert.assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            mvObs = true
            mvObs
        }).start().get())
    }

    @get:Suspendable @set:Suspendable  var mvObs: Boolean? by Delegates.observable<Boolean?>(null) {
        _, old, new ->
        Fiber.sleep(1)
        println("$old -> $new")
    }

    @Test fun testVarObservableGetDelegProp() {
        assertNull(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            mvObs
        }).start().get())
    }

    @Test fun testVarObservableSetDelegProp() {
        Assert.assertTrue(Fiber(scheduler, SuspendableCallable<Boolean> @Suspendable {
            mvObs = true
            mvObs
        }).start().get())
    }
}
