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

import co.paralleluniverse.strands.channels.*;
import static co.paralleluniverse.common.test.Matchers.*;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import static co.paralleluniverse.strands.channels.Selector.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author pron
 */
//@RunWith(Parameterized.class)
public class SelectorTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = new TestWatcher() {
        @Override
        protected void starting(Description desc) {
            if (Debug.isDebug()) {
                System.out.println("STARTING TEST " + desc.getMethodName());
                Debug.record(0, "STARTING TEST " + desc.getMethodName());
            }
        }

        @Override
        public void failed(Throwable e, Description desc) {
            System.out.println("FAILED TEST " + desc.getMethodName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            if (Debug.isDebug() && !(e instanceof OutOfMemoryError)) {
                Debug.record(0, "EXCEPTION IN THREAD " + Thread.currentThread().getName() + ": " + e + " - " + Arrays.toString(e.getStackTrace()));
                Debug.dumpRecorder("~/quasar.dump");
            }
        }

        @Override
        protected void succeeded(Description desc) {
            Debug.record(0, "DONE TEST " + desc.getMethodName());
        }
    };
    final int mailboxSize;
    final OverflowPolicy policy;
    final boolean singleConsumer;
    final boolean singleProducer;
    final ForkJoinPool fjPool;

    public SelectorTest() {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        this.mailboxSize = 0;
        this.policy = OverflowPolicy.BLOCK;
        this.singleConsumer = false;
        this.singleProducer = false;

        Debug.dumpAfter(20000, "channels.log");
    }
//    public SelectorTest(int mailboxSize, OverflowPolicy policy, boolean singleConsumer, boolean singleProducer) {
//        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
//        this.mailboxSize = mailboxSize;
//        this.policy = policy;
//        this.singleConsumer = singleConsumer;
//        this.singleProducer = singleProducer;
//    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                    {5, OverflowPolicy.THROW, true, false},
                    {5, OverflowPolicy.THROW, false, false},
                    {5, OverflowPolicy.BLOCK, true, false},
                    {5, OverflowPolicy.BLOCK, false, false},
                    {1, OverflowPolicy.BLOCK, false, false},
                    {-1, OverflowPolicy.THROW, true, false},
                    {5, OverflowPolicy.DISPLACE, true, false},
                    {0, OverflowPolicy.BLOCK, false, false},});
    }

    private <Message> Channel<Message> newChannel() {
        return Channels.newChannel(mailboxSize, policy, singleProducer, singleConsumer);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSelectReceive1() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                SelectAction<String> sa1 = select(
                        receive(channel1),
                        receive(channel2),
                        receive(channel3));

                String m1 = sa1.message();

                SelectAction<String> sa2 = select(
                        receive(channel1),
                        receive(channel2),
                        receive(channel3));
                String m2 = sa2.message();

                assertThat(sa1.index(), is(0));
                assertThat(m1, equalTo("hello"));
                assertThat(sa2.index(), is(2));
                assertThat(m2, equalTo("world!"));
            }
        }).start();

        Thread.sleep(200);

        channel1.send("hello");
        Thread.sleep(200);

        channel3.send("world!");

        fib.join();
    }

    @Test
    public void testSelectReceive2() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Strand.sleep(200);
                SelectAction<String> sa1 = select(
                        receive(channel1),
                        receive(channel2),
                        receive(channel3));

                String m1 = sa1.message();

                Strand.sleep(200);
                SelectAction<String> sa2 = select(
                        receive(channel1),
                        receive(channel2),
                        receive(channel3));
                String m2 = sa2.message();

                assertThat(sa1.index(), is(0));
                assertThat(m1, equalTo("hello"));
                assertThat(sa2.index(), is(2));
                assertThat(m2, equalTo("world!"));
            }
        }).start();

        channel1.send("hello");
        channel3.send("world!");

        fib.join();
    }

    @Test
    public void testSelectReceiveWithClose1() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                SelectAction<String> sa1 = select(
                        receive(channel1),
                        receive(channel2),
                        receive(channel3));

                String m1 = sa1.message();

                SelectAction<String> sa2 = select(
                        receive(channel1),
                        receive(channel2),
                        receive(channel3));
                String m2 = sa2.message();

                assertThat(sa1.index(), is(2));
                assertThat(m1, nullValue());
                assertThat(m2, nullValue());
            }
        }).start();

        Thread.sleep(200);
        channel3.close();

        Thread.sleep(200);
        channel2.close();

        fib.join();
    }

    @Test
    public void testSelectReceiveWithClose2() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Strand.sleep(200);
                SelectAction<String> sa1 = select(
                        receive(channel1),
                        receive(channel2),
                        receive(channel3));

                String m1 = sa1.message();

                Strand.sleep(200);
                SelectAction<String> sa2 = select(
                        receive(channel1),
                        receive(channel2),
                        receive(channel3));
                String m2 = sa2.message();

                assertThat(sa1.index(), is(2));
                assertThat(m1, nullValue());
                assertThat(m2, nullValue());
            }
        }).start();

        channel3.close();
        channel2.close();

        fib.join();
    }

    @Test
    public void testSelectReceiveTimeout1() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                SelectAction<String> sa1 = select(1, TimeUnit.MILLISECONDS,
                        receive(channel1),
                        receive(channel2),
                        receive(channel3));

                SelectAction<String> sa2 = select(300, TimeUnit.MILLISECONDS,
                        receive(channel1),
                        receive(channel2),
                        receive(channel3));
                String m2 = sa2.message();

                assertThat(sa1, is(nullValue()));
                assertThat(sa2.index(), is(0));
                assertThat(m2, equalTo("hello"));
            }
        }).start();

        Thread.sleep(200);

        channel1.send("hello");

        fib.join();
    }

    @Test
    public void testSelectSend1() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                SelectAction<String> sa1 = select(
                        send(channel1, "hi1"),
                        send(channel2, "hi2"),
                        send(channel3, "hi3"));

                SelectAction<String> sa2 = select(
                        send(channel1, "hi1"),
                        send(channel2, "hi2"),
                        send(channel3, "hi3"));

                assertThat(sa1.index(), is(1));
                assertThat(sa2.index(), is(0));
            }
        }).start();

        Thread.sleep(200);

        String m1 = channel2.receive();
        assertThat(m1, equalTo("hi2"));

        Thread.sleep(200);
        String m2 = channel1.receive();
        assertThat(m2, equalTo("hi1"));

        fib.join();
    }

    @Test
    public void testSelectSend2() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Strand.sleep(200);
                SelectAction<String> sa1 = select(
                        send(channel1, "hi1"),
                        send(channel2, "hi2"),
                        send(channel3, "hi3"));

                Strand.sleep(200);
                SelectAction<String> sa2 = select(
                        send(channel1, "hi1"),
                        send(channel2, "hi2"),
                        send(channel3, "hi3"));

                assertThat(sa1.index(), is(1));
                assertThat(sa2.index(), is(0));
            }
        }).start();

        String m1 = channel2.receive();
        assertThat(m1, equalTo("hi2"));

        String m2 = channel1.receive();
        assertThat(m2, equalTo("hi1"));

        fib.join();
    }
}
