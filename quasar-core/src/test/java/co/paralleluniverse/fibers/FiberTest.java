/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.io.serialization.ByteArraySerializer;
import co.paralleluniverse.strands.Condition;
import co.paralleluniverse.strands.SettableFuture;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author pron
 */
@RunWith(Parameterized.class)
public class FiberTest implements Serializable {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;

    private transient FiberScheduler scheduler;

//    public FiberTest() {
////        this.scheduler = new FiberExecutorScheduler("test", Executors.newFixedThreadPool(1)); 
//        this.scheduler = new FiberForkJoinScheduler("test", 4, null, false);
//    }
    public FiberTest(FiberScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @After
    public void tearDown() {
        // Cannot shutdown FiberScheduler, as it is reused over multiple tests. So these FiberSchedulers and their associated threads will be kept over the whole test run.
//      scheduler.shutdown();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {new FiberForkJoinScheduler("test", 4, null, false)},
            {new FiberExecutorScheduler("test", Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("fiber-scheduler-%d").setDaemon(true).build()))},});
    }

    private static Strand.UncaughtExceptionHandler previousUEH;

    @BeforeClass
    public static void setupClass() {
        previousUEH = Fiber.getDefaultUncaughtExceptionHandler();
        Fiber.setDefaultUncaughtExceptionHandler(new Strand.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Strand s, Throwable e) {
                Exceptions.rethrow(e);
            }
        });
    }

    @AfterClass
    public static void afterClass() {
        // Restore
        Fiber.setDefaultUncaughtExceptionHandler(previousUEH);
    }

    @Before
    public void before() {
//        if (scheduler instanceof FiberForkJoinScheduler)
//            System.out.println("==> " + ((FiberForkJoinScheduler) scheduler).getForkJoinPool().getClass().getSuperclass().getName());
    }

    @Test
    public void testPriority() throws Exception {
        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                
            }
        });

        assertThat(fiber.getPriority(), is(Strand.NORM_PRIORITY));
        
        fiber.setPriority(3);
        assertThat(fiber.getPriority(), is(3));
        
        try {
            fiber.setPriority(Strand.MAX_PRIORITY + 1);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            fiber.setPriority(Strand.MIN_PRIORITY - 1);
            fail();
        } catch (IllegalArgumentException e) {
        }
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
    public void testCancel1() throws Exception {
        final AtomicBoolean started = new AtomicBoolean();
        final AtomicBoolean terminated = new AtomicBoolean();
        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                started.set(true);
                try {
                    Fiber.sleep(100);
                    fail("InterruptedException not thrown");
                } catch (InterruptedException e) {
                }
                terminated.set(true);
            }
        });

        fiber.start();
        Thread.sleep(20);
        fiber.cancel(true);
        fiber.join(5, TimeUnit.MILLISECONDS);
        assertThat(started.get(), is(true));
        assertThat(terminated.get(), is(true));
    }

    @Test
    public void testCancel2() throws Exception {
        final AtomicBoolean started = new AtomicBoolean();
        final AtomicBoolean terminated = new AtomicBoolean();
        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                started.set(true);
                try {
                    Fiber.sleep(100);
                    fail("InterruptedException not thrown");
                } catch (InterruptedException e) {
                }
                terminated.set(true);
            }
        });

        fiber.cancel(true);
        fiber.start();
        Thread.sleep(20);
        try {
            fiber.join(5, TimeUnit.MILLISECONDS);
            fail();
        } catch (CancellationException e) {
        }
        assertThat(started.get(), is(false));
        assertThat(terminated.get(), is(false));
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
    public void testNoLocals() throws Exception { // shitty test
        final ThreadLocal<String> tl1 = new ThreadLocal<>();
        final InheritableThreadLocal<String> tl2 = new InheritableThreadLocal<>();
        tl1.set("foo");
        tl2.set("bar");

        Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                assertThat(tl1.get(), is(nullValue()));
                assertThat(tl2.get(), is(nullValue()));

                tl1.set("koko");
                tl2.set("bubu");

                assertThat(tl1.get(), is("koko"));
                assertThat(tl2.get(), is("bubu"));
            }
        }).setNoLocals(true);
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
                Object token = cond.register();
                try {
                    for (int i = 0; !flag.get(); i++)
                        cond.await(i);
                } finally {
                    cond.unregister(token);
                }
            }
        }).start();

        Thread.sleep(200);

        StackTraceElement[] st = fiber.getStackTrace();

        // Strand.printStackTrace(st, System.err);
        assertThat(st[0].getMethodName(), equalTo("park"));
        boolean found = false;
        for (StackTraceElement ste : st) {
            if (ste.getMethodName().equals("foo")) {
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
                Object token = cond.register();
                try {
                    for (int i = 0; !flag.get(); i++)
                        cond.await(i);
                } finally {
                    cond.unregister(token);
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
                for (StackTraceElement ste : st) {
                    if (ste.getMethodName().equals("foo")) {
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

    @Test
    public void testBadFiberDetection() throws Exception {
        Fiber good = new Fiber("good", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for (int i = 0; i < 100; i++)
                    Strand.sleep(10);
            }
        }).start();

        Fiber bad = new Fiber("bad", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1000);
                for (;;) {
                    if (System.nanoTime() >= deadline)
                        break;
                }
            }
        }).start();

        good.join();
        bad.join();
    }

    @Test
    public void testUncaughtExceptionHandler() throws Exception {
        final AtomicReference<Throwable> t = new AtomicReference<>();

        Fiber<Void> f = new Fiber<Void>() {

            @Override
            protected Void run() throws SuspendExecution, InterruptedException {
                throw new RuntimeException("foo");
            }
        };
        f.setUncaughtExceptionHandler(new Strand.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Strand f, Throwable e) {
                t.set(e);
            }
        });

        f.start();

        try {
            f.join();
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage(), equalTo("foo"));
        }

        assertThat(t.get().getMessage(), equalTo("foo"));
    }

    @Test
    public void testDefaultUncaughtExceptionHandler() throws Exception {
        final AtomicReference<Throwable> t = new AtomicReference<>();

        Fiber<Void> f = new Fiber<Void>() {

            @Override
            protected Void run() throws SuspendExecution, InterruptedException {
                throw new RuntimeException("foo");
            }
        };
        Fiber.setDefaultUncaughtExceptionHandler(new Strand.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Strand f, Throwable e) {
                t.set(e);
            }
        });

        f.start();

        try {
            f.join();
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage(), equalTo("foo"));
        }
        final Throwable th = t.get();

        assertTrue(th != null);
        assertThat(th.getMessage(), equalTo("foo"));
    }

    @Test
    public void testUtilsGet() throws Exception {
        final List<Fiber<String>> fibers = new ArrayList<>();
        final List<String> expectedResults = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int tmpI = i;
            expectedResults.add("testUtilsSequence-" + tmpI);
            fibers.add(new Fiber<>(new SuspendableCallable<String>() {
                @Override
                public String run() throws SuspendExecution, InterruptedException {
                    return "testUtilsSequence-" + tmpI;
                }
            }).start());
        }

        final List<String> results = FiberUtil.get(fibers);
        assertThat(results, equalTo(expectedResults));
    }

    @Test
    public void testUtilsGetWithTimeout() throws Exception {
        final List<Fiber<String>> fibers = new ArrayList<>();
        final List<String> expectedResults = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int tmpI = i;
            expectedResults.add("testUtilsSequence-" + tmpI);
            fibers.add(new Fiber<>(new SuspendableCallable<String>() {
                @Override
                public String run() throws SuspendExecution, InterruptedException {
                    return "testUtilsSequence-" + tmpI;
                }
            }).start());
        }

        final List<String> results = FiberUtil.get(1, TimeUnit.SECONDS, fibers);
        assertThat(results, equalTo(expectedResults));
    }

    @Test(expected = TimeoutException.class)
    public void testUtilsGetZeroWait() throws Exception {
        final List<Fiber<String>> fibers = new ArrayList<>();
        final List<String> expectedResults = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int tmpI = i;
            expectedResults.add("testUtilsSequence-" + tmpI);
            fibers.add(new Fiber<>(new SuspendableCallable<String>() {
                @Override
                public String run() throws SuspendExecution, InterruptedException {
                    return "testUtilsSequence-" + tmpI;
                }
            }).start());
        }

        final List<String> results = FiberUtil.get(0, TimeUnit.SECONDS, fibers);
        assertThat(results, equalTo(expectedResults));
    }

    @Test(expected = TimeoutException.class)
    public void testUtilsGetSmallWait() throws Exception {
        final List<Fiber<String>> fibers = new ArrayList<>();
        final List<String> expectedResults = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int tmpI = i;
            expectedResults.add("testUtilsSequence-" + tmpI);
            fibers.add(new Fiber<>(new SuspendableCallable<String>() {
                @Override
                public String run() throws SuspendExecution, InterruptedException {
                    // increase the sleep time to simulate data coming in then timeout
                    Strand.sleep(tmpI * 3, TimeUnit.MILLISECONDS);
                    return "testUtilsSequence-" + tmpI;
                }
            }).start());
        }

        // must be less than 60 (3 * 20) or else the test could sometimes pass.
        final List<String> results = FiberUtil.get(55, TimeUnit.MILLISECONDS, fibers);
        assertThat(results, equalTo(expectedResults));
    }

    @Test
    public void testSerialization1() throws Exception {
        // com.esotericsoftware.minlog.Log.set(1);
        final SettableFuture<byte[]> buf = new SettableFuture<>();

        Fiber<Integer> f1 = new SerFiber1(scheduler, new SettableFutureFiberWriter(buf)).start();
        Fiber<Integer> f2 = Fiber.unparkSerialized(buf.get(), scheduler);

        assertThat(f2.get(), is(55));
    }

    static class SerFiber1 extends SerFiber<Integer> {
        public SerFiber1(FiberScheduler scheduler, FiberWriter fiberWriter) {
            super(scheduler, fiberWriter);
        }

        @Override
        public Integer run() throws SuspendExecution, InterruptedException {
            int sum = 0;
            for (int i = 1; i <= 10; i++) {
                sum += i;
                if (i == 5) {
                    Fiber.parkAndSerialize(fiberWriter);
                    assert i == 5 && sum == 15;
                }
            }
            return sum;
        }
    }

    @Test
    public void testSerialization2() throws Exception {
        // com.esotericsoftware.minlog.Log.set(1);
        final SettableFuture<byte[]> buf = new SettableFuture<>();

        Fiber<Integer> f1 = new SerFiber2(scheduler, new SettableFutureFiberWriter(buf)).start();
        Fiber<Integer> f2 = Fiber.unparkSerialized(buf.get(), scheduler);

        assertThat(f2.get(), is(55));
    }

    static class SerFiber2 extends Fiber<Integer> {
        public SerFiber2(FiberScheduler scheduler, final FiberWriter fiberWriter) {
            super(scheduler, new SuspendableCallable<Integer>() {

                @Override
                public Integer run() throws SuspendExecution, InterruptedException {
                    int sum = 0;
                    for (int i = 1; i <= 10; i++) {
                        sum += i;
                        if (i == 5) {
                            Fiber.parkAndSerialize(fiberWriter);
                            assert i == 5 && sum == 15;
                        }
                    }
                    return sum;
                }
            });
        }
    }

    @Test
    public void testSerializationWithThreadLocals() throws Exception {
        final ThreadLocal<String> tl1 = new ThreadLocal<>();
        final InheritableThreadLocal<String> tl2 = new InheritableThreadLocal<>();
        tl1.set("foo");
        tl2.set("bar");

        final SettableFuture<byte[]> buf = new SettableFuture<>();

        Fiber<Integer> f1 = new SerFiber3(scheduler, new SettableFutureFiberWriter(buf), tl1, tl2).start();
        Fiber<Integer> f2 = Fiber.unparkSerialized(buf.get(), scheduler);

        assertThat(f2.get(), is(55));
    }

    static class SerFiber3 extends SerFiber<Integer> {
        private final ThreadLocal<String> tl1;
        private final InheritableThreadLocal<String> tl2;

        public SerFiber3(FiberScheduler scheduler, FiberWriter fiberWriter, ThreadLocal<String> tl1, InheritableThreadLocal<String> tl2) {
            super(scheduler, fiberWriter);
            this.tl1 = tl1;
            this.tl2 = tl2;
        }

        @Override
        public Integer run() throws SuspendExecution, InterruptedException {
            assertThat(tl1.get(), is(nullValue()));
            assertThat(tl2.get(), is("bar"));

            tl1.set("koko");
            tl2.set("bubu");

            int sum = 0;
            for (int i = 1; i <= 10; i++) {
                sum += i;
                if (i == 5) {
                    Fiber.parkAndSerialize(fiberWriter);
                    assert i == 5 && sum == 15;
                }
            }

            assertThat(tl1.get(), is("koko"));
            assertThat(tl2.get(), is("bubu"));
            return sum;
        }
    }

    static class SerFiber<V> extends Fiber<V> implements java.io.Serializable {
        protected final transient FiberWriter fiberWriter;

        public SerFiber(FiberScheduler scheduler, SuspendableCallable<V> target, FiberWriter fiberWriter) {
            super(scheduler, target);
            this.fiberWriter = fiberWriter;
        }

        public SerFiber(FiberScheduler scheduler, FiberWriter fiberWriter) {
            super(scheduler);
            this.fiberWriter = fiberWriter;
        }
    }

    @Test
    public void testCustomSerialization() throws Exception {
        // com.esotericsoftware.minlog.Log.set(1);
        final SettableFuture<byte[]> buf = new SettableFuture<>();

        Fiber<Integer> f1 = new CustomSerFiber(scheduler, new SettableFutureCustomWriter(buf)).start();
        Fiber<Integer> f2 = Fiber.unparkSerialized(buf.get(), scheduler);

        assertThat(f2.get(), is(55));
    }

    static class CustomSerFiber extends Fiber<Integer> implements Serializable {
        final private transient CustomFiberWriter writer;

        public CustomSerFiber(FiberScheduler scheduler, CustomFiberWriter writer) {
            super(scheduler);
            this.writer = writer;
        }

        @Override
        public Integer run() throws SuspendExecution, InterruptedException {
            int sum = 0;
            for (int i = 1; i <= 10; i++) {
                sum += i;
                if (i == 5) {
                    Fiber.parkAndCustomSerialize(writer);
                    assert i == 5 && sum == 15;
                }
            }
            return sum;
        }
    }

    static class SettableFutureCustomWriter implements CustomFiberWriter {
        final private transient SettableFuture<byte[]> buf;

        public SettableFutureCustomWriter(SettableFuture<byte[]> buf) {
            this.buf = buf;
        }

        @Override
        public void write(Fiber fiber) {
            buf.set(Fiber.getFiberSerializer().write(fiber));
        }
    }

    static class SettableFutureFiberWriter implements FiberWriter {
        private final transient SettableFuture<byte[]> buf;

        public SettableFutureFiberWriter(SettableFuture<byte[]> buf) {
            this.buf = buf;
        }

        @Override
        public void write(Fiber fiber, ByteArraySerializer ser) {
            buf.set(ser.write(fiber));
        }

//        @Override
//        public void write(byte[] serFiber) {
//            buf.set(serFiber);
//        }
    }
}
