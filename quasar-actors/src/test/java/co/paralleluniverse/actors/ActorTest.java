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
package co.paralleluniverse.actors;

import co.paralleluniverse.actors.behaviors.MessageSelector;
import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactoryBuilder;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.SendPort;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.After;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

/**
 *
 * @author pron
 */
public class ActorTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;

    @BeforeClass
    public static void beforeClass() {
        Debug.dumpAfter(10000);
    }
    static final MailboxConfig mailboxConfig = new MailboxConfig(10, Channels.OverflowPolicy.THROW);
    private FiberScheduler scheduler;

    public ActorTest() {
        scheduler = new FiberForkJoinScheduler("test", 4, null, false);
    }

    @After
    public void tearDown() {
    	  scheduler.shutdown();
    }

    private <Message, V> Actor<Message, V> spawnActor(Actor<Message, V> actor) {
        Fiber fiber = new Fiber("actor", scheduler, actor);
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
    public void whenActorThrowsExceptionThenGetThrowsIt() throws Exception {
        Actor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                throw new RuntimeException("foo");
            }
        });

        try {
            actor.get();
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(RuntimeException.class));
            assertThat(e.getCause().getMessage(), is("foo"));
        }
    }

    @Test
    public void whenActorThrowsExceptionThenGetThrowsItThreadActor() throws Exception {
        Actor<Message, Integer> actor = new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                throw new RuntimeException("foo");
            }
        };

        actor.spawnThread();

        try {
            actor.get();
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(RuntimeException.class));
            assertThat(e.getCause().getMessage(), is("foo"));
        }
    }

    @Test
    public void whenActorReturnsValueThenGetReturnsIt() throws Exception {
        Actor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                return 42;
            }
        });

        assertThat(actor.get(), is(42));
    }

    @Test
    public void whenActorReturnsValueThenGetReturnsItThreadActor() throws Exception {
        Actor<Message, Integer> actor = new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                return 42;
            }
        };

        actor.spawnThread();

        assertThat(actor.get(), is(42));
    }

    @Test
    public void testReceive() throws Exception {
        ActorRef<Message> actor = new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                Message m = receive();
                return m.num;
            }
        }.spawn();

        actor.send(new Message(15));

        assertThat(LocalActor.<Integer>get(actor), is(15));
    }

    @Test
    public void testReceiveThreadActor() throws Exception {
        ActorRef<Message> actor = new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                Message m = receive();
                return m.num;
            }
        }.spawnThread();

        actor.send(new Message(15));

        assertThat(LocalActor.<Integer>get(actor), is(15));
    }

    @Test
    public void testReceiveAfterSleep() throws Exception {
        ActorRef<Message> actor = new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                Message m1 = receive();
                Message m2 = receive();
                return m1.num + m2.num;
            }
        }.spawn();

        actor.send(new Message(25));
        Thread.sleep(200);
        actor.send(new Message(17));

        assertThat(LocalActor.<Integer>get(actor), is(42));
    }

    @Test
    public void testReceiveAfterSleepThreadActor() throws Exception {
        ActorRef<Message> actor = new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                Message m1 = receive();
                Message m2 = receive();
                return m1.num + m2.num;
            }
        }.spawnThread();

        actor.send(new Message(25));
        Thread.sleep(200);
        actor.send(new Message(17));

        assertThat(LocalActor.<Integer>get(actor), is(42));
    }

    private class TypedReceiveA {
    };

    private class TypedReceiveB {
    };

    @Test
    public void testTypedReceive() throws Exception {
        Actor<Object, List<Object>> actor = spawnActor(new BasicActor<Object, List<Object>>(mailboxConfig) {
            @Override
            protected List<Object> doRun() throws InterruptedException, SuspendExecution {
                List<Object> list = new ArrayList<>();
                list.add(receive(TypedReceiveA.class));
                list.add(receive(TypedReceiveB.class));
                return list;
            }
        });
        final TypedReceiveB typedReceiveB = new TypedReceiveB();
        final TypedReceiveA typedReceiveA = new TypedReceiveA();
        actor.ref().send(typedReceiveB);
        Thread.sleep(2);
        actor.ref().send(typedReceiveA);
        assertThat(actor.get(500, TimeUnit.MILLISECONDS), equalTo(Arrays.asList(typedReceiveA, typedReceiveB)));
    }

    @Test
    public void testSelectiveReceive() throws Exception {
        Actor<ComplexMessage, List<Integer>> actor = spawnActor(new BasicActor<ComplexMessage, List<Integer>>(mailboxConfig) {
            @Override
            protected List<Integer> doRun() throws SuspendExecution, InterruptedException {
                final List<Integer> list = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    receive(new MessageProcessor<ComplexMessage, ComplexMessage>() {
                        public ComplexMessage process(ComplexMessage m) throws SuspendExecution, InterruptedException {
                            switch (m.type) {
                                case FOO:
                                    list.add(m.num);
                                    receive(new MessageProcessor<ComplexMessage, ComplexMessage>() {
                                        public ComplexMessage process(ComplexMessage m) throws SuspendExecution, InterruptedException {
                                            switch (m.type) {
                                                case BAZ:
                                                    list.add(m.num);
                                                    return m;
                                                default:
                                                    return null;
                                            }
                                        }
                                    });
                                    return m;
                                case BAR:
                                    list.add(m.num);
                                    return m;
                                case BAZ:
                                    fail();
                                default:
                                    return null;
                            }
                        }
                    });
                }
                return list;
            }
        });

        actor.ref().send(new ComplexMessage(ComplexMessage.Type.FOO, 1));
        actor.ref().send(new ComplexMessage(ComplexMessage.Type.BAR, 2));
        actor.ref().send(new ComplexMessage(ComplexMessage.Type.BAZ, 3));

        assertThat(actor.get(), equalTo(Arrays.asList(1, 3, 2)));
    }

    @Test
    public void testSelectiveReceiveMsgSelector() throws Exception {
        Actor<Object, String> actor = spawnActor(new BasicActor<Object, String>(mailboxConfig) {
            @Override
            protected String doRun() throws SuspendExecution, InterruptedException {
                return receive(MessageSelector.select().ofType(String.class));
            }
        });

        actor.ref().send(1);
        actor.ref().send("hello");

        assertThat(actor.get(), equalTo("hello"));
    }

    @Test
    public void testNestedSelectiveWithEqualMessage() throws Exception {
        Actor<String, String> actor = spawnActor(new BasicActor<String, String>(mailboxConfig) {
            @Override
            protected String doRun() throws SuspendExecution, InterruptedException {
                // return receive(a -> a + receive(b -> b));
                return receive(new MessageProcessor<String, String>() {
                    @Override
                    public String process(String m1) throws SuspendExecution, InterruptedException {
                        return m1 + receive(new MessageProcessor<String, String>() {
                            @Override
                            public String process(String m2) throws SuspendExecution, InterruptedException {
                                return m2;
                            }
                        });
                    }
                });
            }
        });

        String msg = "a";
        actor.ref().send(msg);
        actor.ref().send(msg);

        assertThat(actor.get(), equalTo("aa"));
    }

    @Test
    public void whenLinkedActorDiesDuringSelectiveReceiveThenReceiverDies() throws Exception {
        final Actor<Message, Void> a = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                //noinspection InfiniteLoopStatement
                for (;;)
                    System.out.println(receive());
            }
        });

        final Actor<Object, Void> m = spawnActor(new BasicActor<Object, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                link(a.ref());
                receive(MessageSelector.select().ofType(String.class));
                //noinspection StatementWithEmptyBody
                for (; receive(100, TimeUnit.MILLISECONDS) instanceof Integer;);
                return null;
            }
        });

        try {
            a.getStrand().interrupt();

            m.ref().send(1);
            m.ref().send("hello");

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
    public void whenWatchedActorDiesDuringSelectiveReceiveThenExitMessageDeferred() throws Exception {
        final Actor<Message, Void> a = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                //noinspection InfiniteLoopStatement
                for (;;)
                    System.out.println(receive());
            }
        });

        final AtomicReference<ExitMessage> emr = new AtomicReference<>();
        final Actor<Object, Object[]> m = spawnActor(new BasicActor<Object, Object[]>(mailboxConfig) {
            private Object watch;

            @Override
            protected Object[] doRun() throws SuspendExecution, InterruptedException {
                watch = watch(a.ref());
                final String msg = receive(MessageSelector.select().ofType(String.class));
                Object o;
                //noinspection StatementWithEmptyBody
                for (; (o = receive(100, TimeUnit.MILLISECONDS)) instanceof Integer;);
                return new Object[]{watch, msg, o};
            }

            @Override
            protected Object handleLifecycleMessage(LifecycleMessage m) {
                if (m instanceof ExitMessage) {
                    final ExitMessage em = (ExitMessage) m;
                    if (watch.equals(em.watch) && em.actor.equals(a.ref()))
                        emr.set(em);
                }
                return super.handleLifecycleMessage(m);
            }
        });

        try {
            a.getStrand().interrupt();

            m.ref().send(1);
            m.ref().send("hello");

            final Object[] res = m.get();
            assertNotNull(res[0]);
            assertNotNull(res[1]);
            assertNull(res[2]);
            assertNotNull(emr.get());
            assertEquals(res[1], "hello");
            assertEquals(res[0], emr.get().watch);
            assertEquals(a.ref(), emr.get().actor);
            assertNotNull(emr.get().cause);
            assertEquals(emr.get().cause.getClass(), InterruptedException.class);
        } catch (final Throwable t) {
            fail();
        }
    }

    @Test
    public void whenSimpleReceiveAndTimeoutThenReturnNull() throws Exception {
        Actor<Message, Void> actor = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Message m;
                m = receive(100, TimeUnit.MILLISECONDS);
                assertThat(m.num, is(1));
                m = receive(100, TimeUnit.MILLISECONDS);
                assertThat(m.num, is(2));
                m = receive(100, TimeUnit.MILLISECONDS);
                assertThat(m, is(nullValue()));

                return null;
            }
        });

        actor.ref().send(new Message(1));
        Thread.sleep(20);
        actor.ref().send(new Message(2));
        Thread.sleep(200);
        actor.ref().send(new Message(3));
        actor.join();
    }

    @Test
    public void testTimeoutException() throws Exception {
        Actor<Message, Void> actor = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                try {
                    receive(100, TimeUnit.MILLISECONDS, new MessageProcessor<Message, Message>() {
                        public Message process(Message m) throws SuspendExecution, InterruptedException {
                            fail();
                            return m;
                        }
                    });
                    fail();
                } catch (TimeoutException e) {
                }
                return null;
            }
        });

        Thread.sleep(150);
        actor.ref().send(new Message(1));
        actor.join();
    }

    @Test
    public void testSendSync() throws Exception {
        final Actor<Message, Void> actor1 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Message m;
                m = receive();
                assertThat(m.num, is(1));
                m = receive();
                assertThat(m.num, is(2));
                m = receive();
                assertThat(m.num, is(3));
                return null;
            }
        });

        final Actor<Message, Void> actor2 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Fiber.sleep(20);
                actor1.ref().send(new Message(1));
                Fiber.sleep(10);
                actor1.sendSync(new Message(2));
                actor1.ref().send(new Message(3));
                return null;
            }
        });

        actor1.join();
        actor2.join();
    }

    @Test
    public void testLink() throws Exception {
        Actor<Message, Void> actor1 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Fiber.sleep(100);
                return null;
            }
        });

        Actor<Message, Void> actor2 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                try {
                    for (;;) {
                        receive();
                    }
                } catch (LifecycleException e) {
                }
                return null;
            }
        });

        actor1.link(actor2.ref());

        actor1.join();
        actor2.join();
    }

    @Test
    public void whenUnlinkedAfterDeathButBeforeReceiveThenExitMessageIgnored() throws Exception {
        final Channel<Object>
            sync1 = Channels.newChannel(1),
            sync2 = Channels.newChannel(1);
        final Object ping = new Object();

        final Actor<Message, Void> actor1 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected final Void doRun() throws SuspendExecution, InterruptedException {
                sync1.receive();
                return null;
            }
        });

        final Actor<Message, Void> actor2 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected final Void doRun() throws SuspendExecution, InterruptedException {
                try {
                    sync2.receive();
                    tryReceive();
                } catch (final LifecycleException e) {
                    fail();
                }
                return null;
            }
        });

        actor1.link(actor2.ref());   // Link actor 1 and 2
        sync1.send(ping);            // Let actor 1 go ahead
        actor1.join();               // Wait for actor 1 to terminate

        actor1.unlink(actor2.ref()); // Unlink actor 1 and 2
        sync2.send(ping);            // Let actor 2 go ahead and check the mailbox
        actor2.join();               // Wait for actor 2 to terminate
    }

    @Test
    public void testWatch() throws Exception {
        Actor<Message, Void> actor1 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Fiber.sleep(100);
                return null;
            }
        });

        final AtomicBoolean handlerCalled = new AtomicBoolean(false);

        Actor<Message, Void> actor2 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Message m = receive(200, TimeUnit.MILLISECONDS);
                assertThat(m, is(nullValue()));
                return null;
            }

            @Override
            protected Message handleLifecycleMessage(LifecycleMessage m) {
                super.handleLifecycleMessage(m);
                handlerCalled.set(true);
                return null;
            }
        });

        actor2.watch(actor1.ref());

        actor1.join();
        actor2.join();

        assertThat(handlerCalled.get(), is(true));
    }

    @Test
    public void whenUnwatchedAfterDeathButBeforeReceiveThenExitMessageIgnored() throws Exception {
        final Channel<Object>
            sync1 = Channels.newChannel(1),
            sync2 = Channels.newChannel(1);
        final Object ping = new Object();

        final Actor<Message, Void> actor1 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected final Void doRun() throws SuspendExecution, InterruptedException {
                sync1.receive();
                return null;
            }
        });

        final AtomicBoolean handlerCalled = new AtomicBoolean(false);

        final Actor<Message, Void> actor2 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected final Void doRun() throws SuspendExecution, InterruptedException {
                sync2.receive();
                final Message m = receive(200, TimeUnit.MILLISECONDS);
                assertThat(m, is(nullValue()));
                return null;
            }

            @Override
            protected final Message handleLifecycleMessage(LifecycleMessage m) {
                super.handleLifecycleMessage(m);
                handlerCalled.set(true);
                return null;
            }
        });

        final Object watchId = actor1.watch(actor2.ref());  // Watch actor 2
        sync1.send(ping);                                   // Let actor 1 go ahead
        actor1.join();                                      // Wait for actor 1 to terminate

        actor1.unwatch(actor2.ref(), watchId);              // Unwatch actor 2
        sync2.send(ping);                                   // Let actor 2 go ahead and check the mailbox
        actor2.join();                                      // Wait for actor 2 to terminate

        assertThat(handlerCalled.get(), is(false));
    }

    @Test
    public void testWatchGC() throws Exception {
        Assume.assumeFalse(Debug.isDebug());

        final Actor<Message, Void> actor = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Fiber.sleep(120000);
                return null;
            }
        });
        System.out.println("actor1 is " + actor);
        WeakReference wrActor2 = new WeakReference(spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Fiber.sleep(10);
                final Object watch = watch(actor.ref());
//                unwatch(actor, watch);
                return null;
            }
        }));
        System.out.println("actor2 is " + wrActor2.get());
        for (int i = 0; i < 10; i++) {
            Thread.sleep(10);
            System.gc();
        }
        Thread.sleep(2000);

        assertEquals(null, wrActor2.get());
    }

    @Test
    public void transformingSendChannelIsEqualToActor() throws Exception {
        final ActorRef<Integer> actor = new BasicActor<Integer, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                return null;
            }
        }.spawn();

        SendPort<Integer> ch1 = Channels.filterSend(actor, new Predicate<Integer>() {
            @Override
            public boolean apply(Integer input) {
                return input % 2 == 0;
            }
        });
        SendPort<Integer> ch2 = Channels.mapSend(actor, new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer input) {
                return input + 10;
            }
        });

        assertTrue(ch1.equals(actor));
        assertTrue(actor.equals(ch1));
        assertTrue(ch2.equals(actor));
        assertTrue(actor.equals(ch2));
    }

    @Test
    public void testSpawnWithStrandFactory() throws Exception {
        final AtomicBoolean run = new AtomicBoolean(false);

        Actor<Message, Integer> actor = new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                run.set(true);
                return 3;
            }
        };

        ActorRef a = actor.spawn(new StrandFactoryBuilder().setFiber(null).setNameFormat("my-fiber-%d").build());
        Strand s = LocalActor.getStrand(a);
        assertTrue(s.isFiber());
        assertThat(s.getName(), equalTo("my-fiber-0"));
        assertThat((Integer) LocalActor.get(a), equalTo(3));
        assertThat(run.get(), is(true));
        run.set(false);

        actor = new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                run.set(true);
                return 3;
            }
        };

        a = actor.spawn(new StrandFactoryBuilder().setThread(false).setNameFormat("my-thread-%d").build());
        s = LocalActor.getStrand(a);
        assertTrue(!s.isFiber());
        assertThat(s.getName(), equalTo("my-thread-0"));
        LocalActor.join(a);
        assertThat(run.get(), is(true));
        run.set(false);

        Actor<Message, Integer> actor2 = new BasicActor<Message, Integer>("coolactor", mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                run.set(true);
                return 3;
            }
        };

        a = actor2.spawn(new StrandFactoryBuilder().setFiber(null).setNameFormat("my-fiber-%d").build());
        s = LocalActor.getStrand(a);
        assertTrue(s.isFiber());
        assertThat(s.getName(), equalTo("coolactor"));
        assertThat((Integer) LocalActor.get(a), equalTo(3));
        assertThat(run.get(), is(true));
        run.set(false);

        actor2 = new BasicActor<Message, Integer>("coolactor", mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                run.set(true);
                return 3;
            }
        };

        a = actor2.spawn(new StrandFactoryBuilder().setThread(false).setNameFormat("my-thread-%d").build());
        s = LocalActor.getStrand(a);
        assertTrue(!s.isFiber());
        assertThat(s.getName(), equalTo("coolactor"));
        LocalActor.join(a);
        assertThat(run.get(), is(true));
        run.set(false);
    }

    static class Message {
        final int num;

        public Message(int num) {
            this.num = num;
        }
    }

    static class ComplexMessage {
        enum Type {
            FOO, BAR, BAZ, WAT
        }
        final Type type;
        final int num;

        public ComplexMessage(Type type, int num) {
            this.type = type;
            this.num = num;
        }
    }
}
