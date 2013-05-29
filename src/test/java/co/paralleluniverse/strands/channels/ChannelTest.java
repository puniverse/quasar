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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Ignore;

/**
 *
 * @author pron
 */
public class ChannelTest {
    static final int mailboxSize = 10;
    private ForkJoinPool fjPool;

    public ChannelTest() {
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
        final Channel<String> ch = ObjectChannel.<String>create(mailboxSize);

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
        final Channel<String> ch = ObjectChannel.<String>create(mailboxSize);

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
        final Channel<String> ch = ObjectChannel.<String>create(mailboxSize);

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
        final Channel<String> ch = ObjectChannel.<String>create(mailboxSize);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);

                    ch.send("a message");
                } catch (InterruptedException ex) {
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
    public void whenReceiveNotCalledFromOwnerThenThrowException1() throws Exception {
        final Channel<String> ch = ObjectChannel.<String>create(mailboxSize);

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

                try {
                    ch.receive();
                    fail();
                } catch (Exception e) {
                }
            }
        }).start();

        fib1.join();
        fib2.join();
    }

    @Test
    public void whenReceiveNotCalledFromOwnerThenThrowException2() throws Exception {
        final Channel<String> ch = ObjectChannel.<String>create(mailboxSize);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                String m = ch.receive();

                assertThat(m, equalTo("a message"));
            }
        }).start();

        Thread.sleep(50);
        ch.send("a message");

        try {
            ch.receive();
            fail();
        } catch (Exception e) {
        }

        fib.join();
    }

    @Test
    public void whenReceiveNotCalledFromOwnerThenThrowException3() throws Exception {
        final Channel<String> ch = ObjectChannel.<String>create(mailboxSize);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                Fiber.sleep(100);

                ch.send("a message");

                try {
                    ch.receive();
                    fail();
                } catch (Exception e) {
                }
            }
        }).start();

        String m = ch.receive();

        assertThat(m, equalTo("a message"));

        fib.join();
    }

    @Test
    public void whenReceiveNotCalledFromOwnerThenThrowException4() throws Exception {
        final Channel<String> ch = ObjectChannel.<String>create(mailboxSize);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ch.receiveFromThread();
                } catch (InterruptedException ex) {
                    throw new AssertionError(ex);
                }
            }
        });
        thread.start();

        Thread.sleep(100);
        ch.send("a message");

        try {
            ch.receive();
            fail();
        } catch (Exception e) {
        }
        
        thread.join();
    }

    @Test
    public void testChannelGroupReceive() throws Exception {
        final Channel<String> channel1 = ObjectChannel.<String>create(mailboxSize);
        final Channel<String> channel2 = ObjectChannel.<String>create(mailboxSize);
        final Channel<String> channel3 = ObjectChannel.<String>create(mailboxSize);

        final ChannelGroup<String> group = new ChannelGroup<String>(channel1, channel2, channel3);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
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
        channel1.send("goodbye");
        Thread.sleep(100);
        channel2.send("world!");
        fib.join();
    }

    @Test
    public void testChannelGroupReceiveWithTimeout() throws Exception {
        final Channel<String> channel1 = ObjectChannel.<String>create(mailboxSize);
        final Channel<String> channel2 = ObjectChannel.<String>create(mailboxSize);
        final Channel<String> channel3 = ObjectChannel.<String>create(mailboxSize);

        final ChannelGroup<String> group = new ChannelGroup<String>(channel1, channel2, channel3);

        Fiber fib = new Fiber("fiber", fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                String m1 = group.receive();
                String m2 = channel2.receive();
                String m3 = group.receive(10, TimeUnit.MILLISECONDS);
                String m4 = group.receive(100, TimeUnit.MILLISECONDS);

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
}
