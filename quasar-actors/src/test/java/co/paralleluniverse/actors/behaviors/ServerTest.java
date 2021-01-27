/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.*;
import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channels;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.*;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * These tests are also good tests for sendSync, as they test sendSync (and receive) from both fibers and threads.
 *
 * @author pron
 */
public class ServerTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;

    @After
    public void tearDown() {
        ActorRegistry.clear();
        scheduler.shutdown();
    }

    static final MailboxConfig mailboxConfig = new MailboxConfig(10, Channels.OverflowPolicy.THROW);
    private FiberScheduler scheduler;
    private FiberFactory factory;

    public ServerTest() {
        factory = scheduler = new FiberForkJoinScheduler("test", 4, null, false);
    }

    private Server<Message, Integer, Message> spawnServer(ServerHandler<Message, Integer, Message> server) {
        return new ServerActor<>("server", server).spawn(factory);
    }

    private <T extends Actor<Message, V>, Message, V> T spawnActor(T actor) {
        Fiber fiber = new Fiber(scheduler, actor);
        fiber.setUncaughtExceptionHandler(new Strand.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Strand s, Throwable e) {
                e.printStackTrace();
                throw Exceptions.rethrow(e);
            }
        });
        fiber.start();
        return actor;
    }

    @Test
    public void whenServerStartsThenInitIsCalled() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        Server<Message, Integer, Message> gs = spawnServer(server);

        try {
            LocalActor.join(gs, 100, TimeUnit.MILLISECONDS);
            fail("actor died");
        } catch (TimeoutException e) {
        }

        verify(server).init();
    }

    @Test
    public void whenShutdownIsCalledInInitThenServerStops() throws Exception {
        Server<Message, Integer, Message> gs = spawnServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public void init() {
                ServerActor.currentServerActor().shutdown();
            }
        });

        LocalActor.join(gs, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledThenResultIsReturned() throws Exception {
        final Server<Message, Integer, Message> s = spawnServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public Integer handleCall(ActorRef<?> from, Object id, Message m) {
                ServerActor.currentServerActor().shutdown();
                return m.a + m.b;
            }
        });

        Actor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxConfig) {
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                return s.call(new Message(3, 4));
            }
        });

        int res = actor.get();
        assertThat(res, is(7));

        LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledFromThreadThenResultIsReturned() throws Exception {
        Server<Message, Integer, Message> s = spawnServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public Integer handleCall(ActorRef<?> from, Object id, Message m) {
                ServerActor.currentServerActor().shutdown();
                return m.a + m.b;
            }
        });

        int res = s.call(new Message(3, 4));

        assertThat(res, is(7));

        LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledAndTimeoutThenThrowTimeout() throws Exception {
        Server<Message, Integer, Message> s = spawnServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public Integer handleCall(ActorRef<?> from, Object id, Message m) throws SuspendExecution {
                try {
                    Strand.sleep(50);
                    ServerActor.currentServerActor().shutdown();
                    return m.a + m.b;
                } catch (InterruptedException ex) {
                    System.out.println("?????: " + Arrays.toString(ex.getStackTrace()));
                    return 40;
                }
            }
        });

        try {
            int res = s.call(new Message(3, 4), 10, TimeUnit.MILLISECONDS);
            fail("res: " + res);
        } catch (TimeoutException e) {
        }

        LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testDefaultTimeout1() throws Exception {
        Server<Message, Integer, Message> s = spawnServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public Integer handleCall(ActorRef<?> from, Object id, Message m) throws SuspendExecution {
                try {
                    Strand.sleep(50);
                    ServerActor.currentServerActor().shutdown();
                    return m.a + m.b;
                } catch (InterruptedException ex) {
                    System.out.println("?????: " + Arrays.toString(ex.getStackTrace()));
                    return 40;
                }
            }
        });

        s.setDefaultTimeout(10, TimeUnit.MILLISECONDS);
        try {
            int res = s.call(new Message(3, 4));
            fail("res: " + res);
        } catch (RuntimeException e) {
            assertThat(e.getCause(), instanceOf(TimeoutException.class));
        }

        LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testDefaultTimeout2() throws Exception {
        Server<Message, Integer, Message> s = spawnServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public Integer handleCall(ActorRef<?> from, Object id, Message m) throws SuspendExecution {
                try {
                    Strand.sleep(50);
                    ServerActor.currentServerActor().shutdown();
                    return m.a + m.b;
                } catch (InterruptedException ex) {
                    System.out.println("?????: " + Arrays.toString(ex.getStackTrace()));
                    return 40;
                }
            }
        });

        s.setDefaultTimeout(100, TimeUnit.MILLISECONDS);
        int res = s.call(new Message(3, 4));
        assertThat(res, is(7));

        LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenHandleCallThrowsExceptionThenItPropagatesToCaller() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        when(server.handleCall(any(ActorRef.class), any(), any(Message.class))).thenThrow(new RuntimeException("my exception"));

        final Server<Message, Integer, Message> s = spawnServer(server);

        Actor<Message, Void> actor = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            protected Void doRun() throws SuspendExecution, InterruptedException {
                try {
                    int res = s.call(new Message(3, 4));
                    fail();
                } catch (RuntimeException e) {
                    assertThat(e.getMessage(), equalTo("my exception"));
                }
                return null;
            }
        });

        actor.join();
        try {
            LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
            fail("actor died");
        } catch (TimeoutException e) {
        }
    }

    @Test
    public void whenHandleCallThrowsExceptionThenItPropagatesToThreadCaller() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        when(server.handleCall(any(ActorRef.class), any(), any(Message.class))).thenThrow(new RuntimeException("my exception"));

        final Server<Message, Integer, Message> s = spawnServer(server);

        try {
            int res = s.call(new Message(3, 4));
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), equalTo("my exception"));
        }

        try {
            LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
            fail("actor died");
        } catch (TimeoutException e) {
        }
    }

    @Test
    public void whenActorDiesThenCausePropagatesToThreadCaller() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        doThrow(new RuntimeException("my exception")).when(server).init();

        final Server<Message, Integer, Message> s = spawnServer(server);

        try {
            int res = s.call(new Message(3, 4));
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), equalTo("my exception"));
        }

        try {
            LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage(), equalTo("my exception"));
        }
    }

    @Test
    public void whenLinkedActorDiesDuringCallThenCallerDies() throws Exception {
        final Actor<Message, Void> a = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                //noinspection InfiniteLoopStatement
                for (;;)
                    System.out.println(receive());
            }
        });

        final Server<Message, Integer, Message> s = spawnServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public Integer handleCall(ActorRef<?> from, Object id, Message m) throws SuspendExecution {
                try {
                    a.getStrand().interrupt();
                    Strand.sleep(500);
                } catch (InterruptedException ex) {
                    System.out.println("?????: " + Arrays.toString(ex.getStackTrace()));
                }
                return 0;
            }
        });

        final Actor<Message, Void> m = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                link(a.ref());
                s.call(new Message(3, 4));
                return null;
            }
        });

        try {
            m.join();
            fail();
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            assertEquals(cause.getClass(), LifecycleException.class);
            final LifecycleException lce = (LifecycleException) cause;
            assertEquals(lce.message().getClass(), ExitMessage.class);
            final ExitMessage em = (ExitMessage) lce.message();
            assertEquals(em.getCause().getClass(), InterruptedException.class);
            assertNull(em.watch);
            assertEquals(em.actor, a.ref());
        }
    }

    @Test
    public void whenWatchedActorDiesDuringCallThenExitMessageDeferred() throws Exception {
        final Actor<Message, Void> a = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                //noinspection InfiniteLoopStatement
                for (;;)
                    System.out.println(receive());
            }
        });

        final Server<Message, Integer, Message> s = spawnServer(new AbstractServerHandler<Message, Integer, Message>() {
            @Override
            public Integer handleCall(ActorRef<?> from, Object id, Message m) throws SuspendExecution {
                try {
                    a.getStrand().interrupt();
                    Strand.sleep(500);
                } catch (InterruptedException ex) {
                    System.out.println("?????: " + Arrays.toString(ex.getStackTrace()));
                }
                return 0;
            }
        });

        final AtomicReference<ExitMessage> emr = new AtomicReference<>();
        final Actor<Message, Object[]> m = spawnActor(new BasicActor<Message, Object[]>(mailboxConfig) {
            private Object watch;

            @Override
            protected Object[] doRun() throws SuspendExecution, InterruptedException {
                return new Object[]{
                    watch = watch(a.ref()),
                    s.call(new Message(3, 4)),
                    receive(100, TimeUnit.MILLISECONDS)
                };
            }

            @Override
            protected Message handleLifecycleMessage(LifecycleMessage m) {
                if (m instanceof ExitMessage) {
                    final ExitMessage em = (ExitMessage) m;
                    if (watch.equals(em.watch) && em.actor.equals(a.ref()))
                        emr.set(em);
                }
                return super.handleLifecycleMessage(m);
            }
        });

        try {
            final Object[] res = m.get();
            assertNotNull(res[0]);
            assertNotNull(res[1]);
            assertNull(res[2]);
            assertNotNull(emr.get());
            assertEquals(res[1], 0);
            assertEquals(res[0], emr.get().watch);
            assertEquals(a.ref(), emr.get().actor);
            assertNotNull(emr.get().cause);
            assertEquals(emr.get().cause.getClass(), InterruptedException.class);
        } catch (final Throwable t) {
            fail();
        }
    }

    @Test
    public void whenTimeoutThenHandleTimeoutIsCalled() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        ServerActor<Message, Integer, Message> s = spawnActor(new ServerActor<Message, Integer, Message>() {
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

        s.join(500, TimeUnit.MILLISECONDS); // should be enough
        assertThat(counter.get(), is(5));
    }

    @Test
    public void whenCalledThenDeferredResultIsReturned() throws Exception {
        final Server<Message, Integer, Message> s = new ServerActor<Message, Integer, Message>() {
            private int a, b;
            private ActorRef<?> from;
            private Object id;
            private boolean received;

            @Override
            public void init() throws SuspendExecution {
                setTimeout(50, TimeUnit.MILLISECONDS);
            }

            @Override
            public Integer handleCall(ActorRef<?> from, Object id, Message m) {
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
                return s.call(new Message(3, 4));
            }
        });

        int res = actor.get();
        assertThat(res, is(7));

        LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledFromThreadThenDeferredResultIsReturned() throws Exception {
        final Server<Message, Integer, Message> s = new ServerActor<Message, Integer, Message>() {
            private int a, b;
            private ActorRef<?> from;
            private Object id;
            private boolean received;

            @Override
            public void init() throws SuspendExecution {
                setTimeout(50, TimeUnit.MILLISECONDS);
            }

            @Override
            public Integer handleCall(ActorRef<?> from, Object id, Message m) {
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

        int res = s.call(new Message(3, 4));

        assertThat(res, is(7));
        LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenActorDiesDuringDeferredHandlingThenCausePropagatesToThreadCaller() throws Exception {
        final Server<Message, Integer, Message> s = new ServerActor<Message, Integer, Message>() {
            private boolean received;

            @Override
            public void init() throws SuspendExecution {
                setTimeout(50, TimeUnit.MILLISECONDS);
            }

            @Override
            public Integer handleCall(ActorRef<?> from, Object id, Message m) {
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
            int res = s.call(new Message(3, 4));
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), equalTo("my exception"));
        }

        try {
            LocalActor.join(s, 100, TimeUnit.MILLISECONDS);
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage(), equalTo("my exception"));
        }
    }

    @Test
    public void whenCastThenHandleCastIsCalled() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        final Server<Message, Integer, Message> s = spawnServer(server);

        s.cast(new Message(3, 4));

        s.shutdown();
        LocalActor.join(s);

        verify(server).handleCast(isNull(), any(), eq(new Message(3, 4)));
    }

    @Test
    public void whenSentMessageHandleInfoIsCalled() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        final Server<Message, Integer, Message> s = spawnServer(server);

        s.send("foo");

        s.shutdown();
        LocalActor.join(s);

        verify(server).handleInfo("foo");
    }

    @Test
    public void whenSentShutdownThenTerminateIsCalledAndServerStopped() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);
        final Server<Message, Integer, Message> s = spawnServer(server);

        s.shutdown();
        LocalActor.join(s);

        verify(server).terminate(null);
    }

    @Test
    public void whenHandleInfoThrowsExceptionThenTerminateIsCalled() throws Exception {
        final ServerHandler<Message, Integer, Message> server = mock(ServerHandler.class);

        final Exception myException = new RuntimeException("my exception");
        doThrow(myException).when(server).handleInfo(any());
        final Server<Message, Integer, Message> s = spawnServer(server);

        s.send("foo");

        try {
            LocalActor.join(s);
            fail();
        } catch (Exception e) {
            assertThat(e.getCause().getMessage(), equalTo("my exception"));
        }

        verify(server).terminate(myException);
    }

    @Test
    public void testRegistration() throws Exception {
        Server<Message, Integer, Message> s = new ServerActor<Message, Integer, Message>() {
            @Override
            protected void init() throws SuspendExecution, InterruptedException {
                // Strand.sleep(1000);
                register("my-server");
            }
        }.spawn();

        assertTrue(s == (Server) ActorRegistry.getActor("my-server"));
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
