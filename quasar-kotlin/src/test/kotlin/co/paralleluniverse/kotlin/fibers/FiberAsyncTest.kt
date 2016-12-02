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
package co.paralleluniverse.kotlin.fibers

import co.paralleluniverse.common.util.CheckedCallable
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.FiberAsync
import co.paralleluniverse.fibers.FiberForkJoinScheduler
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.strands.Strand
import co.paralleluniverse.strands.SuspendableRunnable
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * @author pron
 * @author circlespainter
 */
class FiberAsyncTest {
    private val scheduler = FiberForkJoinScheduler("test", 4, null, false)

    private interface MyCallback {
        fun call(str: String)
        fun fail(e: RuntimeException)
    }

    private interface Service {
        fun registerCallback(callback: MyCallback)
    }
    private val syncService = object : Service {
        override fun registerCallback(callback: MyCallback) {
            callback.call("sync result!")
        }
    }
    private val badSyncService = object : Service {
        override fun registerCallback(callback: MyCallback) {
            callback.fail(RuntimeException("sync exception!"))
        }
    }
    private val executor = Executors.newFixedThreadPool(1)
    private val asyncService = object : Service {
        override fun registerCallback(callback: MyCallback) {
            executor.submit {
                try {
                    Strand.sleep(50)
                    callback.call("async result!")
                } catch (ex: InterruptedException) {
                    throw RuntimeException(ex)
                }
            }
        }
    }
    private val longAsyncService = object : Service {
        override fun registerCallback(callback: MyCallback) {
            executor.submit {
                try {
                    Strand.sleep(2000)
                    callback.call("async result!")
                } catch (ex: InterruptedException) {
                    throw RuntimeException(ex)
                }
            }

        }
    }
    private val badAsyncService = object : Service {
        override fun registerCallback(callback: MyCallback) {
            executor.submit {
                try {
                    Strand.sleep(20)
                    callback.fail(RuntimeException("async exception!"))
                } catch (ex: InterruptedException) {
                    throw RuntimeException(ex)
                }
            }
        }
    }

    companion object {
        @Suspendable private fun callService(service: Service): String {
            return object : MyFiberAsync() {
                override fun requestAsync() {
                    service.registerCallback(this)
                }
            }.run()
        }

        @Suspendable private fun callService(service: Service, timeout: Long, unit: TimeUnit): String {
            return object : MyFiberAsync() {
                override fun requestAsync() {
                    service.registerCallback(this)
                }
            }.run(timeout, unit)
        }
    }

    abstract class MyFiberAsync : FiberAsync<String, RuntimeException>(), MyCallback {
        override fun call(str: String) {
            super.asyncCompleted(str)
        }

        override fun fail(e: RuntimeException) {
            super.asyncFailed(e)
        }
    }

    @Test
    fun testSyncCallback() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            val res = callService(syncService)
            assertThat(res, equalTo("sync result!"))
        }).start()

        fiber.join()
    }

    @Test
    fun testSyncCallbackException() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            try {
                callService(badSyncService)
                fail()
            } catch (e: Exception) {
                assertThat(e.message, equalTo("sync exception!"))
            }
        }).start()

        fiber.join()
    }

    @Test
    fun testAsyncCallback() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            val res = callService(asyncService)
            assertThat(res, equalTo("async result!"))
        }).start()

        fiber.join()
    }

    @Test
    fun testAsyncCallbackException() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            try {
                callService(badAsyncService)
                fail()
            } catch (e: Exception) {
                assertThat(e.message, equalTo("async exception!"))
            }
        }).start()

        fiber.join()
    }

    @Test
    fun testAsyncCallbackExceptionInRequestAsync() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            try {
                object : FiberAsync<String, RuntimeException>() {
                    override fun requestAsync() {
                        throw RuntimeException("requestAsync exception!")
                    }
                }.run()
                fail()
            } catch (e: Exception) {
                assertThat(e.message, equalTo("requestAsync exception!"))
            }
        }).start()

        fiber.join()
    }

    @Test
    fun testTimedAsyncCallbackNoTimeout() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            try {
                val res = callService(asyncService, 100, TimeUnit.MILLISECONDS)
                assertThat(res, equalTo("async result!"))
            } catch (e: TimeoutException) {
                throw RuntimeException()
            }
        }).start()

        fiber.join()
    }

    @Test
    fun testTimedAsyncCallbackWithTimeout() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            try {
                callService(asyncService, 10, TimeUnit.MILLISECONDS)
                fail()
            } catch (e: TimeoutException) {}
        }).start()

        fiber.join()
    }

    @Test
    fun testInterrupt1() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            try {
                callService(longAsyncService)
                fail()
            } catch (e: InterruptedException) {}
        }).start()

        fiber.interrupt()
        fiber.join()
    }

    @Test
    fun testInterrupt2() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            try {
                callService(longAsyncService)
                fail()
            } catch (e: InterruptedException) {}
        }).start()

        Thread.sleep(100)
        fiber.interrupt()
        fiber.join()
    }

    @Test
    fun testRunBlocking() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            val res = FiberAsync.runBlocking(Executors.newCachedThreadPool(), CheckedCallable<kotlin.String, java.lang.InterruptedException> @Suspendable {
                Strand.sleep(300)
                "ok"
            })
            assertThat(res, equalTo("ok"))
        }).start()

        fiber.join()
    }

    @Test
    fun testRunBlockingWithTimeout1() {
        val fiber = Fiber<Void>(scheduler, SuspendableRunnable @Suspendable {
            try {
                val res = FiberAsync.runBlocking(Executors.newCachedThreadPool(), 400, TimeUnit.MILLISECONDS, CheckedCallable<kotlin.String, java.lang.InterruptedException> @Suspendable {
                    Strand.sleep(300)
                    "ok"
                })
                assertThat(res, equalTo("ok"))
            } catch (e: TimeoutException) {
                fail()
            }
        }).start()

        fiber.join()
    }

    @Test
    fun testRunBlockingWithTimeout2() {
        val fiber = Fiber<Void>(SuspendableRunnable @Suspendable {
            try {
                FiberAsync.runBlocking(Executors.newCachedThreadPool(), 100, TimeUnit.MILLISECONDS, CheckedCallable<kotlin.String, java.lang.InterruptedException> @Suspendable {
                    Strand.sleep(300)
                    "ok"
                })
                fail()
            } catch (e: TimeoutException) {}
        }).start()

        fiber.join()
    }
}
