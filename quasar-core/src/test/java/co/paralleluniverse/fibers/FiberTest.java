/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.strands.Condition;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jsr166e.ForkJoinPool;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author pron
 */
public class FiberTest {
    private FiberScheduler scheduler;

    public FiberTest() {
        scheduler = new FiberScheduler(new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true));
    }

    @BeforeClass
    public static void setUpClass() {
        Fiber.setDefaultUncaughtExceptionHandler(new Fiber.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Fiber lwt, Throwable e) {
                Exceptions.rethrow(e);
            }
        });
    }

    @Test
    public void testTimeout() throws Exception {
        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                Fiber.park(100, TimeUnit.MILLISECONDS);
            }
        }).start();


        try {
            fiber.join(50, TimeUnit.MILLISECONDS);
            fail();
        } catch (java.util.concurrent.TimeoutException e) {
        }

        fiber.join(200, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testJoinFromFiber() throws Exception {
        final Fiber<Integer> fiber1 = new Fiber<Integer>(scheduler, new SuspendableCallable<Integer>() {
            @Override
            public Integer run() throws SuspendExecution {
                Fiber.park(100, TimeUnit.MILLISECONDS);
                return 123;
            }
        }).start();

        final Fiber<Integer> fiber2 = new Fiber<Integer>(scheduler, new SuspendableCallable<Integer>() {
            @Override
            public Integer run() throws SuspendExecution, InterruptedException {
                try {
                    int res = fiber1.get();
                    return res;
                } catch (ExecutionException e) {
                    throw Exceptions.rethrow(e.getCause());
                }
            }
        }).start();


        int res = fiber2.get();

        assertThat(res, is(123));
        assertThat(fiber1.get(), is(123));
    }

    @Test
    public void testInterrupt() throws Exception {
        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                try {
                    Fiber.sleep(100);
                    fail("InterruptedException not thrown");
                } catch (InterruptedException e) {
                }
            }
        }).start();

        Thread.sleep(20);
        fiber.interrupt();
        fiber.join(5, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testThreadLocals() throws Exception {
        final ThreadLocal<String> tl1 = new ThreadLocal<>();
        final InheritableThreadLocal<String> tl2 = new InheritableThreadLocal<>();
        tl1.set("foo");
        tl2.set("bar");

        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                assertThat(tl1.get(), is(nullValue()));
                assertThat(tl2.get(), is("bar"));

                tl1.set("koko");
                tl2.set("bubu");

                assertThat(tl1.get(), is("koko"));
                assertThat(tl2.get(), is("bubu"));

                Fiber.sleep(100);

                assertThat(tl1.get(), is("koko"));
                assertThat(tl2.get(), is("bubu"));
            }
        });
        fiber.start();
        fiber.join();

        assertThat(tl1.get(), is("foo"));
        assertThat(tl2.get(), is("bar"));
    }

    @Test
    public void testInheritThreadLocals() throws Exception {
        final ThreadLocal<String> tl1 = new ThreadLocal<>();
        tl1.set("foo");

        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                assertThat(tl1.get(), is("foo"));

                Fiber.sleep(100);

                assertThat(tl1.get(), is("foo"));

                tl1.set("koko");

                assertThat(tl1.get(), is("koko"));

                Fiber.sleep(100);

                assertThat(tl1.get(), is("koko"));
            }
        });
        fiber.inheritThreadLocals().start();
        fiber.join();

        assertThat(tl1.get(), is("foo"));
    }

    @Test
    public void testThreadLocalsParallel() throws Exception {
        final ThreadLocal<String> tl = new ThreadLocal<>();

        final int n = 100;
        final int loops = 100;
        Fiber[] fibers = new Fiber[n];
        for (int i = 0; i < n; i++) {
            final int id = i;
            Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    for (int j = 0; j < loops; j++) {
                        final String tlValue = "tl-" + id + "-" + j;
                        tl.set(tlValue);
                        assertThat(tl.get(), equalTo(tlValue));
                        Strand.sleep(10);
                        assertThat(tl.get(), equalTo(tlValue));
                    }
                }
            });
            fiber.start();
            fibers[i] = fiber;
        }

        for (Fiber fiber : fibers)
            fiber.join();
    }

    @Test
    public void testInheritThreadLocalsParallel() throws Exception {
        final ThreadLocal<String> tl = new ThreadLocal<>();
        tl.set("foo");

        final int n = 100;
        final int loops = 100;
        Fiber[] fibers = new Fiber[n];
        for (int i = 0; i < n; i++) {
            final int id = i;
            Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    for (int j = 0; j < loops; j++) {
                        final String tlValue = "tl-" + id + "-" + j;
                        tl.set(tlValue);
                        assertThat(tl.get(), equalTo(tlValue));
                        Strand.sleep(10);
                        assertThat(tl.get(), equalTo(tlValue));
                    }
                }
            }).inheritThreadLocals();
            fiber.start();
            fibers[i] = fiber;
        }

        for (Fiber fiber : fibers)
            fiber.join();
    }

    @Test
    public void whenFiberIsNewThenDumpStackReturnsNull() throws Exception {
        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                foo();
            }

            private void foo() {
            }
        });

        StackTraceElement[] st = fiber.getStackTrace();
        assertThat(st, is(nullValue()));
    }

    @Test
    public void whenFiberIsTerminatedThenDumpStackReturnsNull() throws Exception {
        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                foo();
            }

            private void foo() {
            }
        }).start();

        fiber.join();

        StackTraceElement[] st = fiber.getStackTrace();
        assertThat(st, is(nullValue()));
    }

    @Test
    public void testDumpStackCurrentFiber() throws Exception {
        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                foo();
            }

            private void foo() {
                StackTraceElement[] st = Fiber.currentFiber().getStackTrace();

                // Strand.printStackTrace(st, System.err);

                assertThat(st[0].getMethodName(), equalTo("getStackTrace"));
                assertThat(st[1].getMethodName(), equalTo("foo"));
                assertThat(st[st.length - 1].getMethodName(), equalTo("run"));
                assertThat(st[st.length - 1].getClassName(), equalTo(Fiber.class.getName()));
            }
        }).start();

        fiber.join();
    }

    @Test
    public void testDumpStackRunningFiber() throws Exception {
        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                foo();
            }

            private void foo() {
                final long start = System.nanoTime();
                for (;;) {
                    if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) > 1000)
                        break;
                }
            }
        }).start();

        Thread.sleep(200);

        StackTraceElement[] st = fiber.getStackTrace();

        // Strand.printStackTrace(st, System.err);

        boolean found = false;
        for (int i = 0; i < st.length; i++) {
            if (st[i].getMethodName().equals("foo")) {
                found = true;
                break;
            }
        }
        assertThat(found, is(true));
        assertThat(st[st.length - 1].getMethodName(), equalTo("run"));
        assertThat(st[st.length - 1].getClassName(), equalTo(Fiber.class.getName()));

        fiber.join();
    }

    @Test
    public void testDumpStackWaitingFiber() throws Exception {
        final Condition cond = new SimpleConditionSynchronizer(null);
        final AtomicBoolean flag = new AtomicBoolean(false);

        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                foo();
            }

            private void foo() throws InterruptedException, SuspendExecution {
                cond.register();
                try {
                    for (int i = 0; !flag.get(); i++) {
                        cond.await(i);
                    }
                } finally {
                    cond.unregister();
                }
            }
        }).start();

        Thread.sleep(200);

        StackTraceElement[] st = fiber.getStackTrace();

        // Strand.printStackTrace(st, System.err);

        assertThat(st[0].getMethodName(), equalTo("park"));
        boolean found = false;
        for (int i = 0; i < st.length; i++) {
            if (st[i].getMethodName().equals("foo")) {
                found = true;
                break;
            }
        }
        assertThat(found, is(true));
        assertThat(st[st.length - 1].getMethodName(), equalTo("run"));
        assertThat(st[st.length - 1].getClassName(), equalTo(Fiber.class.getName()));

        flag.set(true);
        cond.signalAll();

        fiber.join();
    }

    @Test
    public void testDumpStackWaitingFiberWhenCalledFromFiber() throws Exception {
        final Condition cond = new SimpleConditionSynchronizer(null);
        final AtomicBoolean flag = new AtomicBoolean(false);

        final Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                foo();
            }

            private void foo() throws InterruptedException, SuspendExecution {
                cond.register();
                try {
                    for (int i = 0; !flag.get(); i++) {
                        cond.await(i);
                    }
                } finally {
                    cond.unregister();
                }
            }
        }).start();

        Thread.sleep(200);

        Fiber fiber2 = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                StackTraceElement[] st = fiber.getStackTrace();

                // Strand.printStackTrace(st, System.err);

                assertThat(st[0].getMethodName(), equalTo("park"));
                boolean found = false;
                for (int i = 0; i < st.length; i++) {
                    if (st[i].getMethodName().equals("foo")) {
                        found = true;
                        break;
                    }
                }
                assertThat(found, is(true));
                assertThat(st[st.length - 1].getMethodName(), equalTo("run"));
                assertThat(st[st.length - 1].getClassName(), equalTo(Fiber.class.getName()));
            }
        }).start();

        fiber2.join();
        
        flag.set(true);
        cond.signalAll();

        fiber.join();
    }

    @Test
    public void testDumpStackSleepingFiber() throws Exception {
        // sleep is a special case
        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                foo();
            }

            private void foo() throws InterruptedException, SuspendExecution {
                Fiber.sleep(1000);
            }
        }).start();

        Thread.sleep(200);

        StackTraceElement[] st = fiber.getStackTrace();

        // Strand.printStackTrace(st, System.err);

        assertThat(st[0].getMethodName(), equalTo("sleep"));
        boolean found = false;
        for (int i = 0; i < st.length; i++) {
            if (st[i].getMethodName().equals("foo")) {
                found = true;
                break;
            }
        }
        assertThat(found, is(true));
        assertThat(st[st.length - 1].getMethodName(), equalTo("run"));
        assertThat(st[st.length - 1].getClassName(), equalTo(Fiber.class.getName()));

        fiber.join();
    }
}
