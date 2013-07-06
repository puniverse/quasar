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
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import java.util.Arrays;
import java.util.Collection;
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
@RunWith(Parameterized.class)
public class ChannelTest {
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

//    public ChannelTest() {
//        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
//        this.mailboxSize = 0;
//        this.policy = OverflowPolicy.BLOCK;
//        this.singleConsumer = false;
//        this.singleProducer = false;
//
//        Debug.dumpAfter(20000, "channels.log");
//    }
    public ChannelTest(int mailboxSize, OverflowPolicy policy, boolean singleConsumer, boolean singleProducer) {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        this.mailboxSize = mailboxSize;
        this.policy = policy;
        this.singleConsumer = singleConsumer;
        this.singleProducer = singleProducer;
    }

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
    public void sendMessageFromFiberToFiber() throws Exception {
        final Channel<String> ch = newChannel();

        Fiber fib1 = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                String m = ch.receive();

                assertThat(m, equalTo("a message"));
            }
        }).start();

        Fiber fib2 = new Fiber("fiber", fjPool, new SuspendableRunnable() {
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

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
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

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
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

    @Ignore
    @Test
    public void whenReceiveNotCalledFromOwnerThenThrowException1() throws Exception {
        assumeTrue(Debug.isAssertionsEnabled());
        final Channel<String> ch = newChannel();

        Fiber fib1 = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                String m = ch.receive();

                assertThat(m, equalTo("a message"));
            }
        }).start();

        Fiber fib2 = new Fiber("fiber", fjPool, new SuspendableRunnable() {
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

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
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

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
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
    public void whenChannelOverflowsThrowException() throws Exception {
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
    public void testBlockingChannelSendingFiber() throws Exception {
        assumeThat(policy, is(OverflowPolicy.BLOCK));
        final Channel<Integer> ch = newChannel();

        Fiber<Integer> receiver = new Fiber<Integer>(fjPool, new SuspendableCallable<Integer>() {
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

        Fiber<Void> sender = new Fiber<Void>(fjPool, new SuspendableRunnable() {
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

        Fiber<Integer> fib = new Fiber<Integer>(fjPool, new SuspendableCallable<Integer>() {
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

        Fiber fib = new Fiber(fjPool, new SuspendableRunnable() {
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
    public void testChannelCloseWithSleep() throws Exception {
        final Channel<Integer> ch = newChannel();

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
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
        Fiber fib1 = new Fiber("fiber", fjPool, r).start();
        Fiber fib2 = new Fiber("fiber", fjPool, r).start();

        Thread.sleep(500);

        ch.close();
        fib1.join();
        fib2.join();
    }

    @Test
    public void testPrimitiveChannelClose() throws Exception {
        assumeThat(mailboxSize, not(equalTo(0)));

        final IntChannel ch = Channels.newIntChannel(mailboxSize, policy);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
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
    public void testTopic() throws Exception {
        final Channel<String> channel1 = newChannel();
        final Channel<String> channel2 = newChannel();
        final Channel<String> channel3 = newChannel();

        final Topic<String> topic = new Topic<String>();

        topic.subscribe(channel1);
        topic.subscribe(channel2);
        topic.subscribe(channel3);


        Fiber f1 = new Fiber(fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                assertThat(channel1.receive(), equalTo("hello"));
                assertThat(channel1.receive(), equalTo("world!"));
            }
        }).start();

        Fiber f2 = new Fiber(fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                assertThat(channel2.receive(), equalTo("hello"));
                assertThat(channel2.receive(), equalTo("world!"));
            }
        }).start();

        Fiber f3 = new Fiber(fjPool, new SuspendableRunnable() {
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
}
