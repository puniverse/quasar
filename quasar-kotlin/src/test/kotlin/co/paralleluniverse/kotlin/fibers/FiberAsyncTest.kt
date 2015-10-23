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
package co.paralleluniverse.kotlin.fibers

import co.paralleluniverse.common.util.CheckedCallable
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.FiberAsync
import co.paralleluniverse.fibers.FiberForkJoinScheduler
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.strands.SuspendableRunnable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.hamcrest.CoreMatchers.*
import org.junit.Ignore

/**
 *
 * @author pron
 */
public class FiberAsyncTest {
    private val scheduler = FiberForkJoinScheduler("test", 4, null, false);

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
            executor.submit(object : Runnable {
                override fun run() {
                    try {
                        Thread.sleep(20)
                        callback.call("async result!")
                    } catch (ex: InterruptedException) {
                        throw RuntimeException(ex)
                    }
                }
            })
        }
    }
    private val longAsyncService = object : Service {
        override fun registerCallback(callback: MyCallback) {
            executor.submit(object : Runnable {
                override fun run() {
                    try {
                        Thread.sleep(2000)
                        callback.call("async result!")
                    } catch (ex: InterruptedException) {
                        throw RuntimeException(ex)
                    }
                }
            })

        }
    }
    private val badAsyncService = object : Service {
        override fun registerCallback(callback: MyCallback) {
            executor.submit(object : Runnable {
                override fun run() {
                    try {
                        Thread.sleep(20);
                        callback.fail(RuntimeException("async exception!"))
                    } catch (ex: InterruptedException) {
                        throw RuntimeException(ex)
                    }
                }
            })
        }
    }

    companion object {
        @Suspendable fun callService(service: Service): String {
            return object : MyFiberAsync() {
                override fun requestAsync() {
                    service.registerCallback(this);
                }
            }.run()
        }

        @Suspendable fun callService(service: Service, timeout: Long, unit: TimeUnit): String {
            return object : MyFiberAsync() {
                override fun requestAsync() {
                    service.registerCallback(this)
                }
            }.run(timeout, unit)
        }
    }

    abstract class MyFiberAsync : FiberAsync<String, RuntimeException>(), MyCallback {
        override fun call(str: String) {
            super<FiberAsync>.asyncCompleted(str)
        }

        override fun fail(e: RuntimeException) {
            super<FiberAsync>.asyncFailed(e)
        }
    }

    @Test
    public fun testSyncCallback() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                val res = callService(syncService)
                assertThat(res, equalTo("sync result!"))
            }
        }).start()

        fiber.join()
    }

    @Test
    public fun testSyncCallbackException() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                try {
                    callService(badSyncService)
                    fail()
                } catch (e: Exception) {
                    assertThat(e.message, equalTo("sync exception!"))
                }
            }
        }).start()

        fiber.join()
    }

    @Test
    public fun testAsyncCallback() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                val res = callService(asyncService)
                assertThat(res, equalTo("async result!"))
            }
        }).start()

        fiber.join()
    }

    @Test
    public fun testAsyncCallbackException() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                try {
                    callService(badAsyncService)
                    fail();
                } catch (e: Exception) {
                    assertThat(e.message, equalTo("async exception!"))
                }
            }
        }).start()

        fiber.join()
    }

    @Test
    public fun testAsyncCallbackExceptionInRequestAsync() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                try {
                    object : FiberAsync<String, RuntimeException>() {
                        override protected fun requestAsync() {
                            throw RuntimeException("requestAsync exception!")
                        }
                    }.run()
                    fail()
                } catch (e: Exception) {
                    assertThat(e.message, equalTo("requestAsync exception!"))
                }
            }
        }).start()

        fiber.join()
    }

    @Test
    public fun testTimedAsyncCallbackNoTimeout() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                try {
                    val res = callService(asyncService, 50, TimeUnit.MILLISECONDS)
                    assertThat(res, equalTo("async result!"))
                } catch (e: TimeoutException) {
                    throw RuntimeException()
                }
            }
        }).start()

        fiber.join();
    }

    @Test
    public fun testTimedAsyncCallbackWithTimeout() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                try {
                    callService(asyncService, 10, TimeUnit.MILLISECONDS)
                    fail()
                } catch (e: TimeoutException) {}
            }
        }).start()

        fiber.join()
    }

    @Test
    public fun testInterrupt1() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                try {
                    callService(longAsyncService)
                    fail()
                } catch (e: InterruptedException) {}
            }
        }).start()

        fiber.interrupt()
        fiber.join()
    }

    @Test
    public fun testInterrupt2() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                try {
                    callService(longAsyncService)
                    fail()
                } catch (e: InterruptedException) {}
            }
        }).start()

        Thread.sleep(100)
        fiber.interrupt()
        fiber.join()
    }

    @Test
    public fun testRunBlocking() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                val res = FiberAsync.runBlocking(Executors.newCachedThreadPool(), object : CheckedCallable<String, InterruptedException> {
                override public fun call(): String {
                    Thread.sleep(300)
                    return "ok"
                }
            })
            assertThat(res, equalTo("ok"))
        }
        }).start()

        fiber.join()
    }

    @Test
    public fun testRunBlockingWithTimeout1() {
        val fiber = Fiber<Void>(scheduler, object : SuspendableRunnable {
            @Suspendable override fun run() {
                try {
                    val res = FiberAsync.runBlocking(Executors.newCachedThreadPool(), 400, TimeUnit.MILLISECONDS, object : CheckedCallable<String, InterruptedException> {
                        override public fun call(): String {
                            Thread.sleep(300)
                            return "ok"
                        }
                    })
                    assertThat(res, equalTo("ok"))
                } catch (e: TimeoutException) {
                    fail();
                }
            }
        }).start();

        fiber.join();
    }

    @Test
    public fun testRunBlockingWithTimeout2() {
        val fiber = Fiber<Void>(object : SuspendableRunnable {
            @Suspendable override fun run() {
                try {
                    FiberAsync.runBlocking(Executors.newCachedThreadPool(), 100, TimeUnit.MILLISECONDS, object : CheckedCallable<String, InterruptedException> {
                        override public fun call(): String {
                            Thread.sleep(300)
                            return "ok"
                        }
                    });
                    fail();
                } catch (e: TimeoutException) {}
            }
        }).start();

        fiber.join();
    }
}
