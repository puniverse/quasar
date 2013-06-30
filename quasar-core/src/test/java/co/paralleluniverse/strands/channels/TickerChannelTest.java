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
package co.paralleluniverse.strands.channels;

import static co.paralleluniverse.common.test.Matchers.*;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.TickerIntChannel.TickerChannelIntConsumer;
import co.paralleluniverse.strands.channels.TickerChannel.TickerChannelConsumer;
import co.paralleluniverse.strands.channels.QueueChannel.OverflowPolicy;
import java.util.Arrays;
import jsr166e.ForkJoinPool;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 *
 * @author pron
 */
public class TickerChannelTest {
    @Rule
    public TestRule watchman = new TestWatcher() {
        @Override
        protected void starting(Description desc) {
            if (Debug.isDebug())
                Debug.record(0, "STARTING TEST " + desc.getMethodName());
        }

        @Override
        public void failed(Throwable e, Description desc) {
            e.printStackTrace(System.err);
            if (Debug.isDebug() && !(e instanceof OutOfMemoryError)) {
                Debug.record(0, "EXCEPTION IN THREAD " + Thread.currentThread().getName() + ": " + e + " - " + Arrays.toString(e.getStackTrace()));
                Debug.dumpRecorder("~/quasar-" + desc.getMethodName() + ".dump");
            }
        }
    };
    static final int bufferSize = 10;
    private ForkJoinPool fjPool;

    public TickerChannelTest() {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void sendMessageFromFiberToFiber() throws Exception {
        final Channel<String> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE);

        Fiber fib1 = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                TickerChannelConsumer<String> ch = TickerChannel.newConsumer(sch);
                String m = ch.receive();

                assertThat(m, equalTo("a message"));
            }
        }).start();

        Fiber fib2 = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Fiber.sleep(50);
                sch.send("a message");
            }
        }).start();

        fib1.join();
        fib2.join();
    }

    @Test
    public void sendMessageFromThreadToFiber() throws Exception {
        final Channel<String> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                TickerChannelConsumer<String> ch = TickerChannel.newConsumer(sch);
                String m = ch.receive();

                assertThat(m, equalTo("a message"));
            }
        }).start();

        Thread.sleep(50);
        sch.send("a message");

        fib.join();
    }

    @Test
    public void sendMessageFromFiberToThread() throws Exception {
        final Channel<String> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Fiber.sleep(100);

                sch.send("a message");
            }
        }).start();

        TickerChannelConsumer<String> ch = TickerChannel.newConsumer(sch);
        String m = ch.receive();

        assertThat(m, equalTo("a message"));

        fib.join();
    }

    @Test
    public void sendMessageFromThreadToThread() throws Exception {
        final Channel<String> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);

                    sch.send("a message");
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
            }
        });
        thread.start();

        TickerChannelConsumer<String> ch = TickerChannel.newConsumer(sch);
        String m = ch.receive();

        assertThat(m, equalTo("a message"));

        thread.join();
    }

    @Test
    public void whenSendNotCalledFromOwnerThenThrowException1() throws Exception {
        assumeTrue(Debug.isAssertionsEnabled());
        final Channel<String> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE, true, false);

        Fiber fib1 = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                sch.send("foo");
            }
        }).start();

        Fiber fib2 = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Fiber.sleep(50);

                boolean thrown = false;
                try {
                    sch.send("bar");
                } catch (Throwable e) {
                    thrown = true;
                }
                assertTrue(thrown);
            }
        }).start();

        fib1.join();
        fib2.join();
    }

    @Test
    public void whenSendNotCalledFromOwnerThenThrowException2() throws Exception {
        assumeTrue(Debug.isAssertionsEnabled());
        final Channel<String> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE, true, false);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                sch.send("foo");
            }
        }).start();

        Thread.sleep(50);

        boolean thrown = false;
        try {
            sch.send("bar");
        } catch (Throwable e) {
            thrown = true;
        }
        assertTrue(thrown);

        fib.join();
    }

    @Test
    public void whenSendNotCalledFromOwnerThenThrowException3() throws Exception {
        assumeTrue(Debug.isAssertionsEnabled());
        final Channel<String> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE, true, false);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Fiber.sleep(100);

                boolean thrown = false;
                try {
                    sch.send("bar");
                } catch (Throwable e) {
                    thrown = true;
                }
                assertTrue(thrown);
            }
        }).start();

        sch.send("foo");

        fib.join();
    }

    @Test
    public void whenSendNotCalledFromOwnerThenThrowException4() throws Exception {
        assumeTrue(Debug.isAssertionsEnabled());
        final Channel<String> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE, true, false);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sch.send("foo");
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        });
        thread.start();

        Thread.sleep(100);

        boolean thrown = false;
        try {
            sch.send("bar");
        } catch (Throwable e) {
            thrown = true;
        }
        assertTrue(thrown);
        
        thread.join();
    }

    @Test
    public void testChannelClose() throws Exception {
        final Channel<Integer> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                TickerChannelConsumer<Integer> ch = TickerChannel.newConsumer(sch);
                for (int i = 1; i <= 5; i++) {
                    Integer m = ch.receive();

                    assertThat(m, equalTo(i));
                }

                Integer m = ch.receive();

                assertThat(m, nullValue());
                assertTrue(ch.isClosed());
            }
        }).start();

        Thread.sleep(50);
        sch.send(1);
        sch.send(2);
        sch.send(3);
        sch.send(4);
        sch.send(5);

        sch.close();

        sch.send(6);
        sch.send(7);

        fib.join();
    }

    @Test
    public void testChannelCloseWithSleep() throws Exception {
        final Channel<Integer> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                TickerChannelConsumer<Integer> ch = TickerChannel.newConsumer(sch);
                for (int i = 1; i <= 5; i++) {
                    Integer m = ch.receive();

                    assertThat(m, equalTo(i));
                }

                Integer m = ch.receive();

                assertThat(m, nullValue());
                assertTrue(ch.isClosed());
            }
        }).start();

        Thread.sleep(50);
        sch.send(1);
        sch.send(2);
        sch.send(3);
        sch.send(4);
        sch.send(5);

        Thread.sleep(50);
        sch.close();

        sch.send(6);
        sch.send(7);

        fib.join();
    }

    @Test
    public void testPrimitiveChannelClose() throws Exception {
        final IntChannel sch = Channels.newIntChannel(bufferSize, OverflowPolicy.DISPLACE);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                TickerChannelIntConsumer ch = TickerIntChannel.newConsumer(sch);
                for (int i = 1; i <= 5; i++) {
                    int m = ch.receiveInt();

                    assertThat(m, is(i));
                }

                try {
                    int m = ch.receiveInt();
                    fail("m = " + m);
                } catch (QueueChannel.EOFException e) {
                }

                assertTrue(ch.isClosed());
            }
        }).start();

        Thread.sleep(50);
        sch.send(1);
        sch.send(2);
        sch.send(3);
        sch.send(4);
        sch.send(5);

        sch.close();

        sch.send(6);
        sch.send(7);

        fib.join();
    }

    @Test
    public void testMultipleConsumersAlwaysAscending() throws Exception {
        final Channel<Integer> sch = Channels.newChannel(bufferSize, OverflowPolicy.DISPLACE);

        final SuspendableRunnable run = new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                // System.out.println(Strand.currentStrand() + ": starting");
                final TickerChannelConsumer<Integer> ch = TickerChannel.newConsumer(sch);
                int prev = -1;
                long prevIndex = -1;
                Integer m;
                while ((m = ch.receive()) != null) {
                    //System.out.println(Strand.currentStrand() + ": " + m);
                    long index = ch.getLastIndexRead();
                    assertThat("index", index, greaterThan(prevIndex));
                    assertThat("message", m.intValue(), greaterThan(prev));

                    prev = m;
                    prevIndex = index;
                }

                assertThat(ch.isClosed(), is(true));
            }
        };

        int i = 1;

        for (; i < 50; i++)
            sch.send(i);
        Fiber f1 = new Fiber(fjPool, run).start();
        Thread t1 = new Thread(Strand.toRunnable(run));
        t1.start();
        for (; i < 200; i++)
            sch.send(i);
        Fiber f2 = new Fiber(fjPool, run).start();
        Thread t2 = new Thread(Strand.toRunnable(run));
        t2.start();
        for (; i < 600; i++)
            sch.send(i);
        Fiber f3 = new Fiber(fjPool, run).start();
        Thread t3 = new Thread(Strand.toRunnable(run));
        t3.start();
        for (; i < 800; i++)
            sch.send(i);
        Fiber f4 = new Fiber(fjPool, run).start();
        Thread t4 = new Thread(Strand.toRunnable(run));
        t4.start();
        for (; i < 2000; i++)
            sch.send(i);

        sch.close();
        System.out.println("done send");

        Debug.dumpAfter(5000, "ticker.log");
        f1.join();
        System.out.println("f1: " + f1);
        f2.join();
        System.out.println("f2: " + f2);
        f3.join();
        System.out.println("f3: " + f3);
        f4.join();
        System.out.println("f4: " + f4);
        t1.join();
        System.out.println("t1: " + t1);
        t2.join();
        System.out.println("t2: " + t2);
        t3.join();
        System.out.println("t3: " + t3);
        t4.join();
        System.out.println("t4: " + t4);
    }
}
