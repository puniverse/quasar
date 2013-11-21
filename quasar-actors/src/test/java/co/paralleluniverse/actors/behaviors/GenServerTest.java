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

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.LocalActorUtil;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channels;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * These tests are also good tests for sendSync, as they test sendSync (and receive) from both fibers and threads.
 *
 * @author pron
 */
public class GenServerTest {
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
    static final MailboxConfig mailboxConfig = new MailboxConfig(10, Channels.OverflowPolicy.THROW);
    private FiberScheduler scheduler;

    public GenServerTest() {
        scheduler = new FiberScheduler("test", 4, null, false);
    }

    private Server<Message, Integer, Message> spawnGenServer(ServerHandler<Message, Integer, Message> server) {
        return new ServerActor<>("server", server).spawn(scheduler);
    }

    private <T extends Actor<Message, V>, Message, V> T spawnActor(T actor) {
        Fiber fiber = new Fiber(scheduler, actor);
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
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        Server<Message, Integer, Message> gs = spawnGenServer(server);

        try {
            LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
            fail("actor died");
        } catch (TimeoutException e) {
        }

        verify(server).init();
    }

    @Test
    public void whenShutdownIsCalledInInitThenServerStops() throws Exception {
        Server<Message, Integer, Message> gs = spawnGenServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public void init() {
                ServerActor.currentGenServer().shutdown();
            }
        });

        LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledThenResultIsReturned() throws Exception {
        final Server<Message, Integer, Message> gs = spawnGenServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public Integer handleCall(ActorRef<Integer> from, Object id, Message m) {
                ServerActor.currentGenServer().shutdown();
                return m.a + m.b;
            }
        });

        Actor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxConfig) {
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                return gs.call(new Message(3, 4));
            }
        });

        int res = actor.get();
        assertThat(res, is(7));

        LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledFromThreadThenResultIsReturned() throws Exception {
        Server<Message, Integer, Message> gs = spawnGenServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public Integer handleCall(ActorRef<Integer> from, Object id, Message m) {
                ServerActor.currentGenServer().shutdown();
                return m.a + m.b;
            }
        });

        int res = gs.call(new Message(3, 4));

        assertThat(res, is(7));

        LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledAndTimeoutThenThrowTimeout() throws Exception {
        Server<Message, Integer, Message> gs = spawnGenServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public Integer handleCall(ActorRef<Integer> from, Object id, Message m) throws SuspendExecution {
                try {
                    Strand.sleep(50);
                    ServerActor.currentGenServer().shutdown();
                    return m.a + m.b;
                } catch (InterruptedException ex) {
                    System.out.println("?????: " + Arrays.toString(ex.getStackTrace()));
                    return 40;
                }
            }
        });

        try {
            int res = gs.call(new Message(3, 4), 10, TimeUnit.MILLISECONDS);
            fail("res: " + res);
        } catch (TimeoutException e) {
        }

        LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenHandleCallThrowsExceptionThenItPropagatesToCaller() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        when(server.handleCall(any(ActorRef.class), anyObject(), any(Message.class))).thenThrow(new RuntimeException("my exception"));

        final Server<Message, Integer, Message> gs = spawnGenServer(server);

        Actor<Message, Void> actor = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
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
            LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
            fail("actor died");
        } catch (TimeoutException e) {
        }
    }

    @Test
    public void whenHandleCallThrowsExceptionThenItPropagatesToThreadCaller() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        when(server.handleCall(any(ActorRef.class), anyObject(), any(Message.class))).thenThrow(new RuntimeException("my exception"));

        final Server<Message, Integer, Message> gs = spawnGenServer(server);

        try {
            int res = gs.call(new Message(3, 4));
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), equalTo("my exception"));
        }

        try {
            LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
            fail("actor died");
        } catch (TimeoutException e) {
        }
    }

    @Test
    public void whenActorDiesThenCausePropagatesToThreadCaller() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        doThrow(new RuntimeException("my exception")).when(server).init();

        final Server<Message, Integer, Message> gs = spawnGenServer(server);

        try {
            int res = gs.call(new Message(3, 4));
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), equalTo("my exception"));
        }

        try {
            LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage(), equalTo("my exception"));
        }
    }

    @Test
    public void whenTimeoutThenHandleTimeoutIsCalled() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        ServerActor<Message, Integer, Message> gs = spawnActor(new ServerActor<Message, Integer, Message>() {
            @Override
            protected void init() {
                setTimeout(20, TimeUnit.MILLISECONDS);
            }

            @Override
            protected void handleTimeout() {
                counter.incrementAndGet();
                if (counter.get() >= 5)
                    shutdown();
            }
        });

        gs.join(500, TimeUnit.MILLISECONDS); // should be enough
        assertThat(counter.get(), is(5));
    }

    @Test
    public void whenCalledThenDeferredResultIsReturned() throws Exception {
        final Server<Message, Integer, Message> gs = new ServerActor<Message, Integer, Message>() {
            private int a, b;
            private ActorRef<Integer> from;
            private Object id;
            private boolean received;

            @Override
            public void init() throws SuspendExecution {
                setTimeout(50, TimeUnit.MILLISECONDS);
            }

            @Override
            public Integer handleCall(ActorRef<Integer> from, Object id, Message m) {
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
                    shutdown();
                }
            }
        }.spawn();

        Actor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxConfig) {
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                return gs.call(new Message(3, 4));
            }
        });

        int res = actor.get();
        assertThat(res, is(7));

        LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledFromThreadThenDeferredResultIsReturned() throws Exception {
        final Server<Message, Integer, Message> gs = new ServerActor<Message, Integer, Message>() {
            private int a, b;
            private ActorRef<Integer> from;
            private Object id;
            private boolean received;

            @Override
            public void init() throws SuspendExecution {
                setTimeout(50, TimeUnit.MILLISECONDS);
            }

            @Override
            public Integer handleCall(ActorRef<Integer> from, Object id, Message m) {
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
                    shutdown();
                }
            }
        }.spawn();

        int res = gs.call(new Message(3, 4));

        assertThat(res, is(7));
        LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenActorDiesDuringDeferredHandlingThenCausePropagatesToThreadCaller() throws Exception {
        final Server<Message, Integer, Message> gs = new ServerActor<Message, Integer, Message>() {
            private boolean received;

            @Override
            public void init() throws SuspendExecution {
                setTimeout(50, TimeUnit.MILLISECONDS);
            }

            @Override
            public Integer handleCall(ActorRef<Integer> from, Object id, Message m) {
                this.received = true;
                return null;
            }

            @Override
            protected void handleTimeout() throws SuspendExecution {
                if (received)
                    throw new RuntimeException("my exception");
            }
        }.spawn();

        try {
            int res = gs.call(new Message(3, 4));
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), equalTo("my exception"));
        }

        try {
            LocalActorUtil.join(gs, 100, TimeUnit.MILLISECONDS);
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage(), equalTo("my exception"));
        }
    }

    @Test
    public void whenCastThenHandleCastIsCalled() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        final Server<Message, Integer, Message> gs = spawnGenServer(server);

        gs.cast(new Message(3, 4));

        gs.shutdown();
        LocalActorUtil.join(gs);

        verify(server).handleCast(any(ActorRef.class), anyObject(), eq(new Message(3, 4)));
    }

    @Test
    public void whenSentMessageHandleInfoIsCalled() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        final Server<Message, Integer, Message> gs = spawnGenServer(server);

        gs.send("foo");

        gs.shutdown();
        LocalActorUtil.join(gs);

        verify(server).handleInfo("foo");
    }

    @Test
    public void whenSentShutdownThenTerminateIsCalledAndServerStopped() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        final Server<Message, Integer, Message> gs = spawnGenServer(server);

        gs.shutdown();
        LocalActorUtil.join(gs);

        verify(server).terminate(null);
    }

    @Test
    public void whenHandleInfoThrowsExceptionThenTerminateIsCalled() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);

        final Exception myException = new RuntimeException("my exception");
        doThrow(myException).when(server).handleInfo(anyObject());
        final Server<Message, Integer, Message> gs = spawnGenServer(server);

        gs.send("foo");

        try {
            LocalActorUtil.join(gs);
            fail();
        } catch (Exception e) {
            assertThat(e.getCause().getMessage(), equalTo("my exception"));
        }

        verify(server).terminate(myException);
    }

    static class Message {
        final int a;
        final int b;

        public Message(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 43 * hash + this.a;
            hash = 43 * hash + this.b;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Message other = (Message) obj;
            if (this.a != other.a)
                return false;
            if (this.b != other.b)
                return false;
            return true;
        }
    }
}
