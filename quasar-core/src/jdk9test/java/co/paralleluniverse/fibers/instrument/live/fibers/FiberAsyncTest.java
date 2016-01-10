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
package co.paralleluniverse.fibers.instrument.live.fibers;

import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.*;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.vtime.ScaledClock;
import co.paralleluniverse.vtime.SystemClock;
import co.paralleluniverse.vtime.VirtualClock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

/**
 *
 * @author pron
 */
public class FiberAsyncTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;

    @BeforeClass
    public static void setupClass() {
        VirtualClock.setForCurrentThreadAndChildren(Debug.isCI() ? new ScaledClock(0.3) : SystemClock.instance());
        System.out.println("Using clock: " + VirtualClock.get());
    }

    @AfterClass
    public static void afterClass() {
        VirtualClock.setGlobal(SystemClock.instance());
    }

    private FiberScheduler scheduler;

    public FiberAsyncTest() {
        scheduler = new FiberForkJoinScheduler("test", 4, null, false);
    }

    interface MyCallback {
        void call(String str);

        void fail(RuntimeException e);
    }

    interface Service {
        void registerCallback(MyCallback callback);
    }
    final Service syncService = callback -> callback.call("sync result!");
    final Service badSyncService = callback -> callback.fail(new RuntimeException("sync exception!"));
    final ExecutorService executor = Executors.newFixedThreadPool(1);
    final Service asyncService = callback -> executor.submit((Runnable) () -> {
        try {
            Thread.sleep(20);
            callback.call("async result!");
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    });
    final Service longAsyncService = callback -> executor.submit((Runnable) () -> {
        try {
            Thread.sleep(2000);
            callback.call("async result!");
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    });
    final Service badAsyncService = callback -> executor.submit((Runnable) () -> {
        try {
            Thread.sleep(20);
            callback.fail(new RuntimeException("async exception!"));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    });

    static String callService(final Service service) throws InterruptedException {
        return new MyFiberAsync() {
            @Override
            protected void requestAsync() {
                service.registerCallback(this);
            }
        }.run();
    }

    static String callService(final Service service, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        return new MyFiberAsync() {
            @Override
            protected void requestAsync() {
                service.registerCallback(this);
            }
        }.run(timeout, unit);
    }

    static abstract class MyFiberAsync extends FiberAsync<String, RuntimeException> implements MyCallback {
        @Override
        public void call(String str) {
            super.asyncCompleted(str);
        }

        @Override
        public void fail(RuntimeException e) {
            super.asyncFailed(e);
        }
    }

    @Test
    public void testSyncCallback() throws Exception {
        final Fiber fiber = new Fiber(scheduler, (SuspendableRunnable) () -> {
            final String res = callService(syncService);
            assertThat(res, equalTo("sync result!"));
        }).start();

        fiber.join();
    }

    @Test
    public void testSyncCallbackException() throws Exception {
        final Fiber fiber = new Fiber(scheduler, (SuspendableRunnable) () -> {
            try {
                callService(badSyncService);
                fail();
            } catch (Exception e) {
                assertThat(e.getMessage(), equalTo("sync exception!"));
            }
        }).start();

        fiber.join();
    }

    @Test
    public void testAsyncCallback() throws Exception {
        final Fiber fiber = new Fiber(scheduler, (SuspendableRunnable) () -> {
            final String res = callService(asyncService);
            assertThat(res, equalTo("async result!"));
        }).start();

        fiber.join();
    }

    @Test
    public void testAsyncCallbackException() throws Exception {
        final Fiber fiber = new Fiber(scheduler, (SuspendableRunnable) () -> {
            try {
                callService(badAsyncService);
                fail();
            } catch (Exception e) {
                assertThat(e.getMessage(), equalTo("async exception!"));
            }
        }).start();

        fiber.join();
    }

    @Test
    public void testAsyncCallbackExceptionInRequestAsync() throws Exception {
        final Fiber fiber = new Fiber(scheduler, (SuspendableRunnable) () -> {
            try {
                new FiberAsync<String, RuntimeException>() {
                    @Override
                    protected void requestAsync() {
                        throw new RuntimeException("requestAsync exception!");
                    }
                }.run();
                fail();
            } catch (Exception e) {
                assertThat(e.getMessage(), equalTo("requestAsync exception!"));
            }
        }).start();

        fiber.join();
    }

    @Test
    public void testTimedAsyncCallbackNoTimeout() throws Exception {
        final Fiber fiber = new Fiber(scheduler, (SuspendableRunnable) () -> {
            try {
                final String res = callService(asyncService, 50, TimeUnit.MILLISECONDS);
                assertThat(res, equalTo("async result!"));
            } catch (TimeoutException e) {
                throw new RuntimeException();
            }
        }).start();

        fiber.join();
    }

    @Test
    public void testTimedAsyncCallbackWithTimeout() throws Exception {
        final Fiber fiber = new Fiber(scheduler, (SuspendableRunnable) () -> {
            try {
                callService(asyncService, 10, TimeUnit.MILLISECONDS);
                fail();
            } catch (TimeoutException ignored) {
            }
        }).start();

        fiber.join();
    }

    @Test
    public void testInterrupt1() throws Exception {
        final Fiber fiber = new Fiber(scheduler, (SuspendableRunnable) () -> {
            try {
                callService(longAsyncService);
                fail();
            } catch (InterruptedException ignored) {
            }
        }).start();

        fiber.interrupt();
        fiber.join();
    }

    @Test
    public void testInterrupt2() throws Exception {
        final Fiber fiber = new Fiber(scheduler, (SuspendableRunnable) () -> {
            try {
                callService(longAsyncService);
                fail();
            } catch (InterruptedException ignored) {
            }
        }).start();

        Thread.sleep(100);
        fiber.interrupt();
        fiber.join();
    }

    @Test
    public void testRunBlocking() throws Exception {
        final Fiber fiber = new Fiber((SuspendableRunnable) () -> {
            final String res = FiberAsync.runBlocking(Executors.newCachedThreadPool(), new CheckedCallable<String, InterruptedException>() {
                @Override
                public String call() throws InterruptedException {
                    Thread.sleep(300);
                    return "ok";
                }
            });
            assertThat(res, equalTo("ok"));
        }).start();

        fiber.join();
    }

    @Test
    public void testRunBlockingWithTimeout1() throws Exception {
        final Fiber fiber = new Fiber((SuspendableRunnable) () -> {
            try {
                final String res = FiberAsync.runBlocking(Executors.newCachedThreadPool(), 400, TimeUnit.MILLISECONDS, new CheckedCallable<String, InterruptedException>() {
                    @Override
                    public String call() throws InterruptedException {
                        Thread.sleep(300);
                        return "ok";
                    }
                });
                assertThat(res, equalTo("ok"));
            } catch (TimeoutException e) {
                fail();
            }
        }).start();

        fiber.join();
    }

    @Test
    public void testRunBlockingWithTimeout2() throws Exception {
        final Fiber fiber = new Fiber((SuspendableRunnable) () -> {
            try {
                FiberAsync.runBlocking(Executors.newCachedThreadPool(), 100, TimeUnit.MILLISECONDS, new CheckedCallable<String, InterruptedException>() {
                    @Override
                    public String call() throws InterruptedException {
                        Thread.sleep(300);
                        return "ok";
                    }
                });
                fail();
            } catch (TimeoutException ignored) {
            }
        }).start();

        fiber.join();
    }
}
