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
package co.paralleluniverse.strands.channels;

import static co.paralleluniverse.common.test.Matchers.*;
import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author pron
 */
@RunWith(Parameterized.class)
public class ChannelTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;

    final int mailboxSize;
    final OverflowPolicy policy;
    final boolean singleConsumer;
    final boolean singleProducer;
    final FiberScheduler scheduler;

//    public ChannelTest() {
//        scheduler = new FiberForkJoinScheduler("test", 4, null, false);
//        this.mailboxSize = 5;
//        this.policy = OverflowPolicy.THROW;
//        this.singleConsumer = true;
//        this.singleProducer = false;
//
//        //Debug.dumpAfter(20000, "channels.log");
//    }
    public ChannelTest(int mailboxSize, OverflowPolicy policy, boolean singleConsumer, boolean singleProducer) {
        scheduler = new FiberForkJoinScheduler("test", 4, null, false);
        this.mailboxSize = mailboxSize;
        this.policy = policy;
        this.singleConsumer = singleConsumer;
        this.singleProducer = singleProducer;
    }

    @Parameterized.Parameters(name = "{0} {1} {2} {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {5, OverflowPolicy.THROW, true, false},
            {5, OverflowPolicy.THROW, false, false},
            {5, OverflowPolicy.BLOCK, true, false},
            {5, OverflowPolicy.BLOCK, false, false},
            {5, OverflowPolicy.DROP, true, false},
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
        scheduler.shutdown();
    }

    @Test
    public void sendMessageFromFiberToFiber() throws Exception {
        final Channel<String> ch = newChannel();

        Fiber fib1 = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                String m = ch.receive();

                assertThat(m, equalTo("a message"));
            }
        }).start();

        Fiber fib2 = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Fiber.sleep(50);
                ch.send("a message");
            }
        }).start();

        fib1.join();
        fib2.join();
    }

    @Test
    public void sendMessageFromThreadToFiber() throws Exception {
        final Channel<String> ch = newChannel();

        Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                String m = ch.receive();

                assertThat(m, equalTo("a message"));
            }
        }).start();

        Thread.sleep(50);
        ch.send("a message");

        fib.join();
    }

    @Test
    public void sendMessageFromFiberToThread() throws Exception {
        final Channel<String> ch = newChannel();

        Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Fiber.sleep(100);

                ch.send("a message");
            }
        }).start();

        String m = ch.receive();

        assertThat(m, equalTo("a message"));

        fib.join();
    }

    @Test
    public void sendMessageFromThreadToThread() throws Exception {
        final Channel<String> ch = newChannel();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);

                    ch.send("a message");
                } catch (InterruptedException | SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }
        });
        thread.start();

        String m = ch.receive();

        assertThat(m, equalTo("a message"));

        thread.join();
    }

    @Test
    public void sendMessageThreadToThreadTimeoutOK() throws Exception {
        final Channel<String> ch = newChannel();

        assumeTrue(mailboxSize == 1);
        assumeTrue(OverflowPolicy.BLOCK.equals(policy));

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);

                    ch.send("message 1");
                    ch.send("message 2", 100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }
        });
        thread.start();

        Thread.sleep(150);

        String m = ch.receive();
        assertThat(m, equalTo("message 1"));
        m = ch.receive(100, TimeUnit.MILLISECONDS);
        assertThat(m, equalTo("message 2"));

        thread.join();
    }

    @Test
    public void sendMessageThreadToThreadTimeoutKO() throws Exception {
        final Channel<String> ch = newChannel();

        assumeTrue(mailboxSize == 1);
        assumeTrue(OverflowPolicy.BLOCK.equals(policy));

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);

                    ch.send("message 1");
                    ch.send("message 2", 100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | SuspendExecution ex) {
                    throw new AssertionError(ex);
                }
            }
        });
        thread.start();

        Thread.sleep(250);

        String m = ch.receive();
        assertThat(m, equalTo("message 1"));
        m = ch.receive(100, TimeUnit.MILLISECONDS);
        assertNull(m);

        thread.join();
    }

    @Ignore
    @Test
    public void whenReceiveNotCalledFromOwnerThenThrowException1() throws Exception {
        assumeTrue(Debug.isAssertionsEnabled());
        final Channel<String> ch = newChannel();

        Fiber fib1 = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                String m = ch.receive();

                assertThat(m, equalTo("a message"));
            }
        }).start();

        Fiber fib2 = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Fiber.sleep(50);
                ch.send("a message");

                boolean thrown = false;
                try {
                    ch.receive();
                } catch (Throwable e) {
                    thrown = true;
                }
                assertTrue(thrown);
            }
        }).start();

        fib1.join();
        fib2.join();
    }

    @Ignore
    @Test
    public void whenReceiveNotCalledFromOwnerThenThrowException2() throws Exception {
        assumeTrue(Debug.isAssertionsEnabled());
        final Channel<String> ch = newChannel();

        Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                String m = ch.receive();

                assertThat(m, equalTo("a message"));
            }
        }).start();

        Thread.sleep(50);
        ch.send("a message");

        boolean thrown = false;
        try {
            ch.receive();
        } catch (Throwable e) {
            thrown = true;
        }
        assertTrue(thrown);

        fib.join();
    }

    @Ignore
    @Test
    public void whenReceiveNotCalledFromOwnerThenThrowException3() throws Exception {
        assumeTrue(Debug.isAssertionsEnabled());
        final Channel<String> ch = newChannel();

        Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Fiber.sleep(100);

                ch.send("a message");

                boolean thrown = false;
                try {
                    ch.receive();
                } catch (Throwable e) {
                    thrown = true;
                }
                assertTrue(thrown);
            }
        }).start();

        String m = ch.receive();

        assertThat(m, equalTo("a message"));

        fib.join();
    }

    @Ignore
    @Test
    public void whenReceiveNotCalledFromOwnerThenThrowException4() throws Exception {
        assumeTrue(Debug.isAssertionsEnabled());
        final Channel<String> ch = newChannel();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ch.receive();
                } catch (InterruptedException ex) {
                    throw new AssertionError(ex);
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }
        });
        thread.start();

        Thread.sleep(100);
        ch.send("a message");

        boolean thrown = false;
        try {
            ch.receive();
        } catch (Throwable e) {
            thrown = true;
        }
        assertTrue(thrown);

        thread.join();
    }

    @Test
    public void whenChannelOverflowsAndPolicyThrowThenThrowException() throws Exception {
        assumeThat(policy, is(OverflowPolicy.THROW));
        assumeThat(mailboxSize, greaterThan(0));

        final Channel<Integer> ch = newChannel();

        int i = 0;
        try {
            for (i = 0; i < 10; i++)
                ch.send(i);
            fail();
        } catch (QueueCapacityExceededException e) {
            System.out.println("i = " + i);
        }
    }

    @Test
    public void whenChannelOverflowsAndPolicyDropThenDrop() throws Exception {
        assumeThat(policy, is(OverflowPolicy.DROP));
        assumeThat(mailboxSize, greaterThan(0));

        final Channel<Integer> ch = newChannel();

        for (int i = 0; i < 10; i++)
            ch.send(i);
    }

    @Test
    public void testBlockingChannelSendingFiber() throws Exception {
        assumeThat(policy, is(OverflowPolicy.BLOCK));
        final Channel<Integer> ch = newChannel();

        Fiber<Integer> receiver = new Fiber<>(scheduler, new SuspendableCallable<Integer>() {
            @Override
            public Integer run() throws SuspendExecution, InterruptedException {
                int i = 0;
                while (ch.receive() != null) {
                    i++;
                    Fiber.sleep(50);
                }
                return i;

            }
        }).start();

        Fiber<Void> sender = new Fiber<Void>(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for (int i = 0; i < 10; i++)
                    ch.send(i);
                ch.close();
            }
        }).start();

        try {
            assertThat(receiver.get(), is(10));
            sender.join();
        } catch (Throwable t) {
            Debug.dumpRecorder("channels.log");
            throw t;
        }
    }

    @Test
    public void testBlockingChannelSendingThread() throws Exception {
        assumeThat(policy, is(OverflowPolicy.BLOCK));
        final Channel<Integer> ch = newChannel();

        Fiber<Integer> fib = new Fiber<>(scheduler, new SuspendableCallable<Integer>() {
            @Override
            public Integer run() throws SuspendExecution, InterruptedException {
                int i = 0;
                while (ch.receive() != null) {
                    i++;
                    Fiber.sleep(50);
                }
                return i;

            }
        }).start();

        for (int i = 0; i < 10; i++)
            ch.send(i);
        ch.close();

        assertThat(fib.get(), is(10));
    }

    @Test
    public void testChannelClose() throws Exception {
        final Channel<Integer> ch = newChannel();

        Fiber fib = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
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
        ch.send(1);
        ch.send(2);
        ch.send(3);
        ch.send(4);
        ch.send(5);

        ch.close();

        ch.send(6);
        ch.send(7);

        fib.join();
    }

    @Test
    public void testChannelCloseException() throws Exception {
        final Channel<Integer> ch = newChannel();

        Fiber fib = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for (int i = 1; i <= 5; i++) {
                    Integer m = ch.receive();

                    assertThat(m, equalTo(i));
                }

                try {
                    ch.receive();
                    fail();
                } catch (ProducerException e) {
                    assertThat(e.getCause().getMessage(), equalTo("foo"));
                }
                assertTrue(ch.isClosed());
            }
        }).start();

        Thread.sleep(50);
        ch.send(1);
        ch.send(2);
        ch.send(3);
        ch.send(4);
        ch.send(5);

        ch.close(new Exception("foo"));

        ch.send(6);
        ch.send(7);

        fib.join();
    }

    @Test
    public void testChannelCloseWithSleep() throws Exception {
        final Channel<Integer> ch = newChannel();

        Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
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
        ch.send(1);
        ch.send(2);
        ch.send(3);
        ch.send(4);
        ch.send(5);

        Thread.sleep(50);
        ch.close();

        ch.send(6);
        ch.send(7);

        fib.join();
    }

    @Test
    public void testChannelCloseExceptionWithSleep() throws Exception {
        final Channel<Integer> ch = newChannel();

        Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for (int i = 1; i <= 5; i++) {
                    Integer m = ch.receive();

                    assertThat(m, equalTo(i));
                }

                try {
                    Integer m = ch.receive();
                    fail("m = " + m);
                } catch (ProducerException e) {
                    assertThat(e.getCause().getMessage(), equalTo("foo"));
                }
                assertTrue(ch.isClosed());
            }
        }).start();

        Thread.sleep(50);
        ch.send(1);
        ch.send(2);
        ch.send(3);
        ch.send(4);
        ch.send(5);

        Thread.sleep(50);
        ch.close(new Exception("foo"));

        ch.send(6);
        ch.send(7);

        fib.join();
    }

    @Test
    public void whenChannelClosedThenBlockedSendsComplete() throws Exception {
        assumeThat(policy, is(OverflowPolicy.BLOCK));
        final Channel<Integer> ch = newChannel();

        final SuspendableRunnable r = new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for (int i = 1; i <= 100; i++) {
                    ch.send(i);
                }
            }
        };
        Fiber fib1 = new Fiber("fiber", scheduler, r).start();
        Fiber fib2 = new Fiber("fiber", scheduler, r).start();

        Thread.sleep(500);

        ch.close();
        fib1.join();
        fib2.join();
    }

    @Test
    public void whenChannelClosedExceptionThenBlockedSendsComplete() throws Exception {
        assumeThat(policy, is(OverflowPolicy.BLOCK));
        final Channel<Integer> ch = newChannel();

        final SuspendableRunnable r = new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for (int i = 1; i <= 100; i++) {
                    ch.send(i);
                }
            }
        };
        Fiber fib1 = new Fiber("fiber", scheduler, r).start();
        Fiber fib2 = new Fiber("fiber", scheduler, r).start();

        Thread.sleep(500);

        ch.close(new Exception("foo"));
        fib1.join();
        fib2.join();
    }

    @Test
    public void testPrimitiveChannelClose() throws Exception {
        assumeThat(mailboxSize, not(equalTo(0)));

        final IntChannel ch = Channels.newIntChannel(mailboxSize, policy);

        Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    for (int i = 1; i <= 5; i++) {
                        int m = ch.receiveInt();

                        assertThat(m, is(i));
                    }
                } catch (QueueChannel.EOFException e) {
                    fail();
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
        ch.send(1);
        ch.send(2);
        ch.send(3);
        ch.send(4);
        ch.send(5);

        ch.close();

        ch.send(6);
        ch.send(7);

        fib.join();
    }

    @Test
    public void testPrimitiveChannelCloseException() throws Exception {
        assumeThat(mailboxSize, not(equalTo(0)));

        final IntChannel ch = Channels.newIntChannel(mailboxSize, policy);

        Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    for (int i = 1; i <= 5; i++) {
                        int m = ch.receiveInt();

                        assertThat(m, is(i));
                    }
                } catch (QueueChannel.EOFException e) {
                    fail();
                }

                try {
                    int m = ch.receiveInt();
                    fail("m = " + m);
                } catch (ProducerException e) {
                    assertThat(e.getCause().getMessage(), equalTo("foo"));
                } catch (ReceivePort.EOFException e) {
                    fail();
                }

                assertTrue(ch.isClosed());
            }
        }).start();

        Thread.sleep(50);
        ch.send(1);
        ch.send(2);
        ch.send(3);
        ch.send(4);
        ch.send(5);

        ch.close(new Exception("foo"));

        ch.send(6);
        ch.send(7);

        fib.join();
    }

    @Test
    public void testTopic() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        final Topic<String> topic = new Topic<>();

        topic.subscribe(channel1);
        topic.subscribe(channel2);
        topic.subscribe(channel3);

        Fiber f1 = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                assertThat(channel1.receive(), equalTo("hello"));
                assertThat(channel1.receive(), equalTo("world!"));
            }
        }).start();

        Fiber f2 = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                assertThat(channel2.receive(), equalTo("hello"));
                assertThat(channel2.receive(), equalTo("world!"));
            }
        }).start();

        Fiber f3 = new Fiber(scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                assertThat(channel3.receive(), equalTo("hello"));
                assertThat(channel3.receive(), equalTo("world!"));
            }
        }).start();

        Thread.sleep(100);
        topic.send("hello");
        Thread.sleep(100);
        topic.send("world!");

        f1.join();
        f2.join();
        f3.join();
    }

    @Test
    public void testChannelGroupReceive() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        final ReceivePortGroup<String> group = new ReceivePortGroup<>(channel1, channel2, channel3);

        Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                String m1 = group.receive();
                String m2 = channel2.receive();

                assertThat(m1, equalTo("hello"));
                assertThat(m2, equalTo("world!"));
            }
        }).start();

        Thread.sleep(100);
        channel3.send("hello");
        Thread.sleep(100);
        if (policy != OverflowPolicy.BLOCK) {
            channel1.send("goodbye"); // TransferChannel will block here
            Thread.sleep(100);
        }
        channel2.send("world!");
        fib.join();
    }

    @Test
    public void testChannelGroupReceiveWithTimeout() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        final ReceivePortGroup<String> group = new ReceivePortGroup<>(channel1, channel2, channel3);

        Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                String m1 = group.receive();
                String m2 = channel2.receive();
                String m3 = group.receive(10, TimeUnit.MILLISECONDS);
                String m4 = group.receive(200, TimeUnit.MILLISECONDS);

                assertThat(m1, equalTo("hello"));
                assertThat(m2, equalTo("world!"));
                assertThat(m3, nullValue());
                assertThat(m4, equalTo("foo"));
            }
        }).start();

        Thread.sleep(100);
        channel3.send("hello");
        Thread.sleep(100);
        channel2.send("world!");
        Thread.sleep(100);
        channel1.send("foo");
        fib.join();
    }

    @Test
    public void testChannelGroupMix() throws Exception {
        assumeThat(mailboxSize, greaterThan(1));

        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();
        final Channel<Object> sync = Channels.newChannel(0);

        final ReceivePortGroup<String> group = new ReceivePortGroup<>();
        group.add(channel3);

        final Fiber fib = new Fiber("fiber", scheduler, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                final String m1 = group.receive();
                final String m2 = channel2.receive();

                sync.receive(); // 1
                sync.receive(); // 2
                final String m3 = group.receive(10, TimeUnit.MILLISECONDS);
                final String m4 = group.receive(200, TimeUnit.MILLISECONDS);

                sync.receive(); // 3
                sync.receive(); // 4
                final String m5 = group.receive(10, TimeUnit.MILLISECONDS);
                final String m6 = group.receive(200, TimeUnit.MILLISECONDS);

                assertThat(m1, equalTo("hello"));
                assertThat(m2, equalTo("world!"));
                assertThat(m3, nullValue());
                assertThat(m4, equalTo("foo"));
                assertThat(m5, nullValue());
                assertThat(m6, equalTo("bar"));

                sync.receive(); // 5
                sync.receive(); // 6
                final String m7 = group.receive();
                assertThat(m7, equalTo("2-solo-sings"));

                sync.receive(); // 7
                sync.receive(); // 8
                String m8 = group.receive();
                while (m8 != null && m8.contains("leaks"))
                    m8 = group.receive();
                String m9 = group.receive();
                while (m9 != null && m9.contains("leaks"))
                    m9 = group.receive();
                String m10 = group.receive();
                while (m10 != null && m10.contains("leaks"))
                    m10 = group.receive();
                assertThat(ImmutableSet.of(m8, m9, m10), equalTo(ImmutableSet.of("1-paused-by-2-solo-waits", "3-paused-by-2-solo-waits", "1-normal")));

                sync.receive(); // 9
                sync.receive(); // 10
                String m11 = group.receive();
                while (m11 != null && m11.contains("leaks"))
                    m11 = group.receive();
                assertThat(m11, equalTo("2-solo-sings-again"));

                sync.receive(); // 11
                sync.receive(); // 12
                String m12 = group.receive();
                while (m12 != null && m12.contains("leaks"))
                    m12 = group.receive();
                assertThat(m12, nullValue());
            }
        }).start();

        final Object ping = new Object();

        Thread.sleep(100);
        channel3.send("hello", 1, TimeUnit.SECONDS);
        Thread.sleep(100);
        channel2.send("world!", 1, TimeUnit.SECONDS);
        sync.send(ping, 1, TimeUnit.SECONDS); // 1

        group.remove(channel3);
        group.add(channel1);
        sync.send(ping, 1, TimeUnit.SECONDS); // 2

        Thread.sleep(150);
        channel1.send("foo", 1, TimeUnit.SECONDS);
        sync.send(ping, 1, TimeUnit.SECONDS); // 3

        group.remove(channel1);
        group.add(channel2);
        sync.send(ping, 1, TimeUnit.SECONDS); // 4

        Thread.sleep(100);
        channel2.send("bar", 1, TimeUnit.SECONDS);
        sync.send(ping, 1, TimeUnit.SECONDS); // 5

        // Solo and mute, solo wins and others are paused (default)
        group.add(channel1);
        group.add(channel3);
        group.setState(new Mix.State(Mix.Mode.MUTE, true), channel2);
        sync.send(ping, 1, TimeUnit.SECONDS); // 6

        channel1.send("1-paused-by-2-solo-waits", 1, TimeUnit.SECONDS);
        channel3.send("3-paused-by-2-solo-waits", 1, TimeUnit.SECONDS);
        channel2.send("2-solo-sings", 1, TimeUnit.SECONDS);
        sync.send(ping, 1, TimeUnit.SECONDS); // 7

        // Remove solo
        group.setState(new Mix.State(false), channel2);
        sync.send(ping, 1, TimeUnit.SECONDS); // 8

        channel2.send("2-muted-leaks", 1, TimeUnit.SECONDS);
        channel1.send("1-normal", 1, TimeUnit.SECONDS);
        sync.send(ping, 1, TimeUnit.SECONDS); // 9

        // Restore normal state and solo and change solo effect on other channels to MUTE
        group.setState(new Mix.State(Mix.Mode.NORMAL, true), channel2);
        group.setSoloEffect(Mix.SoloEffect.MUTE_OTHERS);
        sync.send(ping, 1, TimeUnit.SECONDS); // 10

        channel1.send("1-muted-by-2-solo-leaks", 1, TimeUnit.SECONDS);
        channel3.send("3-muted-by-2-solo-leaks", 1, TimeUnit.SECONDS);
        channel2.send("2-solo-sings-again", 1, TimeUnit.SECONDS);
        sync.send(ping, 1, TimeUnit.SECONDS); // 11

        channel1.close();
        channel2.close();
        channel3.close();
        sync.send(ping, 1, TimeUnit.SECONDS); // 12

        fib.join();
    }
}
