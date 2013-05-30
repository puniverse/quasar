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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import jsr166e.ForkJoinPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * These tests are also good tests for sendSync, as they test sendSync (and receive) from both fibers and threads.
 *
 * @author pron
 */
public class GenServerTest {
    static final int mailboxSize = 10;
    private ForkJoinPool fjPool;

    public GenServerTest() {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    private LocalGenServer<Message, Integer, Message> spawnGenServer(Server<Message, Integer, Message> server) {
        return spawnActor(new LocalGenServer<>("server", server));
    }

    private <T extends LocalActor<Message, V>, Message, V> T spawnActor(T actor) {
        Fiber fiber = new Fiber(fjPool, actor);
        fiber.setUncaughtExceptionHandler(new Fiber.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Fiber lwt, Throwable e) {
                e.printStackTrace();
                throw Exceptions.rethrow(e);
            }
        });
        fiber.start();
        return actor;
    }

    @Test
    public void whenGenServerStartsThenInitIsCalled() throws Exception {
        final AtomicBoolean called = new AtomicBoolean(false);

        LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public void init() {
                called.set(true);
            }
        });

        try {
            gs.join(100, TimeUnit.MILLISECONDS);
            fail("actor died");
        } catch (TimeoutException e) {
        }
        assertThat(called.get(), is(true));
    }

    @Test
    public void whenStopIsCalledInInitThenServerStops() throws Exception {
        LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public void init() {
                LocalGenServer.currentGenServer().stop();
            }
        });

        gs.join(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledThenResultIsReturned() throws Exception {
        final LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public Integer handleCall(Actor<Integer> from, Object id, Message m) {
                LocalGenServer.currentGenServer().stop();
                return m.a + m.b;
            }
        });

        LocalActor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxSize) {
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                return gs.call(new Message(3, 4));
            }
        });

        int res = actor.get();
        assertThat(res, is(7));

        gs.join(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledFromThreadThenResultIsReturned() throws Exception {
        LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public Integer handleCall(Actor<Integer> from, Object id, Message m) {
                LocalGenServer.currentGenServer().stop();
                return m.a + m.b;
            }
        });

        int res = gs.call(new Message(3, 4));

        assertThat(res, is(7));
        gs.join(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledAndTimeoutThenThrowTimeout() throws Exception {
        LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public Integer handleCall(Actor<Integer> from, Object id, Message m) throws SuspendExecution {
                try {
                    LocalGenServer.currentGenServer().stop();
                    Strand.sleep(50);
                    return m.a + m.b;
                } catch (InterruptedException ex) {
                    System.out.println("?????: " + Arrays.toString(ex.getStackTrace()));
                    return 40;
                }
            }
        });

        try {
            int res = gs.call(new Message(3, 4), 10, TimeUnit.MILLISECONDS);
            fail();
        } catch (TimeoutException e) {
        }

        gs.join(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenHandleCallThrowsExceptionThenItPropagatesToCaller() throws Exception {
        final LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public Integer handleCall(Actor<Integer> from, Object id, Message m) {
                throw new RuntimeException("my exception");
            }
        });

        LocalActor<Message, Void> actor = spawnActor(new BasicActor<Message, Void>(mailboxSize) {
            protected Void doRun() throws SuspendExecution, InterruptedException {
                try {
                    int res = gs.call(new Message(3, 4));
                    fail();
                } catch (RuntimeException e) {
                    assertThat(e.getMessage(), equalTo("my exception"));
                }
                return null;
            }
        });

        actor.join();
        try {
            gs.join(100, TimeUnit.MILLISECONDS);
            fail("actor died");
        } catch (TimeoutException e) {
        }
    }

    @Test
    public void whenHandleCallThrowsExceptionThenItPropagatesToThreadCaller() throws Exception {
        LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public Integer handleCall(Actor<Integer> from, Object id, Message m) {
                throw new RuntimeException("my exception");
            }
        });

        try {
            int res = gs.call(new Message(3, 4));
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), equalTo("my exception"));
        }

        try {
            gs.join(100, TimeUnit.MILLISECONDS);
            fail("actor died");
        } catch (TimeoutException e) {
        }
    }

    @Test
    public void whenTimeoutThenHandleTimeoutIsCalled() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        LocalGenServer<Message, Integer, Message> gs = spawnActor(new LocalGenServer<Message, Integer, Message>() {
            @Override
            protected void init() {
                setTimeout(20, TimeUnit.MILLISECONDS);
            }

            @Override
            protected void handleTimeout() {
                counter.incrementAndGet();
                if (counter.get() >= 5)
                    stop();
            }
        });

        gs.join(500, TimeUnit.MILLISECONDS); // should be enough
        assertThat(counter.get(), is(5));
    }

    @Test
    public void whenCalledThenDeferredResultIsReturned() throws Exception {
        final LocalGenServer<Message, Integer, Message> gs = spawnActor(new LocalGenServer<Message, Integer, Message>() {
            private int a, b;
            private Actor<Integer> from;
            private Object id;
            private boolean received;

            @Override
            public void init() throws SuspendExecution {
                setTimeout(50, TimeUnit.MILLISECONDS);
            }

            @Override
            public Integer handleCall(Actor<Integer> from, Object id, Message m) {
                // save for later
                this.a = m.a;
                this.b = m.b;
                this.from = from;
                this.id = id;
                this.received = true;
                return null;
            }

            @Override
            protected void handleTimeout() throws SuspendExecution {
                if (received) {
                    reply(from, id, a + b);
                    stop();
                }
            }
        });

        LocalActor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxSize) {
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                return gs.call(new Message(3, 4));
            }
        });

        int res = actor.get();
        assertThat(res, is(7));

        gs.join(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledFromThreadThenDeferredResultIsReturned() throws Exception {
        final LocalGenServer<Message, Integer, Message> gs = spawnActor(new LocalGenServer<Message, Integer, Message>() {
            private int a, b;
            private Actor<Integer> from;
            private Object id;
            private boolean received;

            @Override
            public void init() throws SuspendExecution {
                setTimeout(50, TimeUnit.MILLISECONDS);
            }

            @Override
            public Integer handleCall(Actor<Integer> from, Object id, Message m) {
                // save for later
                this.a = m.a;
                this.b = m.b;
                this.from = from;
                this.id = id;
                this.received = true;
                return null;
            }

            @Override
            protected void handleTimeout() throws SuspendExecution {
                if (received) {
                    reply(from, id, a + b);
                    stop();
                }
            }
        });

        int res = gs.call(new Message(3, 4));

        assertThat(res, is(7));
        gs.join(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCastThenHandleCastIsCalled() throws Exception {
        final AtomicInteger result = new AtomicInteger();

        LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public void handleCast(Actor<Integer> from, Object id, Message m) {
                LocalGenServer.currentGenServer().stop();
                result.set(m.a * m.b);
            }
        });

        gs.cast(new Message(3, 4));
        gs.join();

        assertThat(result.get(), is(12));
    }

    @Test
    public void whenSentMessageHandleInfoIsCalled() throws Exception {
        final AtomicReference<Object> result = new AtomicReference<Object>();

        LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public void handleInfo(Object m) {
                LocalGenServer.currentGenServer().stop();
                result.set(m);
            }
        });

        gs.send("foo");
        gs.join();

        assertThat(result.get(), equalTo((Object) "foo"));
    }

    @Test
    public void whenSentShutdownThenTerminateIsCalledAndServerStopped() throws Exception {
        final AtomicBoolean called = new AtomicBoolean(false);

        LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public void terminate(Throwable cause) throws SuspendExecution {
                called.set(true);
                assertThat(cause, nullValue());
            }
        });

        gs.shutdown();
        gs.join();

        assertThat(called.get(), is(true));
    }

    @Test
    public void whenHandleInfoThrowsExceptionThenTerminateIsCalled() throws Exception {
        final AtomicBoolean called = new AtomicBoolean(false);

        LocalGenServer<Message, Integer, Message> gs = spawnGenServer(new AbstractServer<Message, Integer, Message>() {
            @Override
            public void handleInfo(Object m) {
                throw new RuntimeException("my exception");
            }

            @Override
            public void terminate(Throwable cause) throws SuspendExecution {
                called.set(true);
                assertThat(cause.getMessage(), equalTo("my exception"));
            }
        });

        gs.send("foo");

        try {
            gs.join();
            fail();
        } catch (Exception e) {
            assertThat(e.getCause().getMessage(), equalTo("my exception"));
        }

        assertThat(called.get(), is(true));
    }

    static class Message {
        final int a;
        final int b;

        public Message(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }
}
