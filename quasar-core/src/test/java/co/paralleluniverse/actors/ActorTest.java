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
package co.paralleluniverse.actors;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import jsr166e.ForkJoinPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author pron
 */
public class ActorTest {
    static final MailboxConfig mailboxConfig = new MailboxConfig(10, MailboxConfig.OverflowPolicy.KILL);
    private ForkJoinPool fjPool;

    public ActorTest() {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    private <Message, V> LocalActor<Message, V> spawnActor(LocalActor<Message, V> actor) {
        Fiber fiber = new Fiber("actor", fjPool, actor);
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
    public void whenActorThrowsExceptionThenGetThrowsIt() throws Exception {

        LocalActor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxConfig) {
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
    public void whenActorReturnsValueThenGetReturnsIt() throws Exception {
        LocalActor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                return 42;
            }
        });

        assertThat(actor.get(), is(42));
    }

    @Test
    public void testReceive() throws Exception {
        LocalActor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                Message m = receive();
                return m.num;
            }
        });

        actor.send(new Message(15));

        assertThat(actor.get(), is(15));
    }

    @Test
    public void testReceiveAfterSleep() throws Exception {
        LocalActor<Message, Integer> actor = spawnActor(new BasicActor<Message, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                Message m1 = receive();
                Message m2 = receive();
                return m1.num + m2.num;
            }
        });

        actor.send(new Message(25));
        Thread.sleep(200);
        actor.send(new Message(17));

        assertThat(actor.get(), is(42));
    }

    @Test
    public void testSelectiveReceive() throws Exception {
        LocalActor<ComplexMessage, List<Integer>> actor = spawnActor(new BasicActor<ComplexMessage, List<Integer>>(mailboxConfig) {
            @Override
            protected List<Integer> doRun() throws SuspendExecution, InterruptedException {
                final List<Integer> list = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    receive(new MessageProcessor<ComplexMessage>() {
                        public boolean process(ComplexMessage m) throws SuspendExecution, InterruptedException {
                            switch (m.type) {
                                case FOO:
                                    list.add(m.num);
                                    receive(new MessageProcessor<ComplexMessage>() {
                                        public boolean process(ComplexMessage m) throws SuspendExecution, InterruptedException {
                                            switch (m.type) {
                                                case BAZ:
                                                    list.add(m.num);
                                                    return true;
                                                default:
                                                    return false;
                                            }
                                        }
                                    });
                                    return true;
                                case BAR:
                                    list.add(m.num);
                                    return true;
                                case BAZ:
                                    fail();
                                default:
                                    return false;
                            }
                        }
                    });
                }
                return list;
            }
        });

        actor.send(new ComplexMessage(ComplexMessage.Type.FOO, 1));
        actor.send(new ComplexMessage(ComplexMessage.Type.BAR, 2));
        actor.send(new ComplexMessage(ComplexMessage.Type.BAZ, 3));

        assertThat(actor.get(), equalTo(Arrays.asList(1, 3, 2)));
    }

    @Test
    public void whenSimpleReceiveAndTimeoutThenReturnNull() throws Exception {
        LocalActor<Message, Void> actor = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Message m;
                m = receive(50, TimeUnit.MILLISECONDS);
                assertThat(m.num, is(1));
                m = receive(50, TimeUnit.MILLISECONDS);
                assertThat(m.num, is(2));
                m = receive(50, TimeUnit.MILLISECONDS);
                assertThat(m, is(nullValue()));

                return null;
            }
        });

        actor.send(new Message(1));
        Thread.sleep(20);
        actor.send(new Message(2));
        Thread.sleep(100);
        actor.send(new Message(3));
        actor.join();
    }

    @Test
    public void testTimeoutException() throws Exception {
        LocalActor<Message, Void> actor = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                try {
                    receive(100, TimeUnit.MILLISECONDS, new MessageProcessor<Message>() {
                        public boolean process(Message m) throws SuspendExecution, InterruptedException {
                            fail();
                            return true;
                        }
                    });
                    fail();
                } catch (TimeoutException e) {
                }
                return null;
            }
        });

        Thread.sleep(150);
        actor.send(new Message(1));
        actor.join();
    }

    @Test
    public void testSendSync() throws Exception {
        final LocalActor<Message, Void> actor1 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
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

        final LocalActor<Message, Void> actor2 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Fiber.sleep(20);
                actor1.send(new Message(1));
                Fiber.sleep(10);
                actor1.sendSync(new Message(2));
                actor1.send(new Message(3));
                return null;
            }
        });

        actor1.join();
        actor2.join();
    }

    @Test
    public void testLink() throws Exception {
        LocalActor<Message, Void> actor1 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Fiber.sleep(100);
                return null;
            }
        });

        LocalActor<Message, Void> actor2 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
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

        actor1.link(actor2);

        actor1.join();
        actor2.join();
    }

    @Test
    public void testWatch() throws Exception {
        LocalActor<Message, Void> actor1 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Fiber.sleep(100);
                return null;
            }
        });

        final AtomicBoolean handlerCalled = new AtomicBoolean(false);

        LocalActor<Message, Void> actor2 = spawnActor(new BasicActor<Message, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                Message m = receive(200, TimeUnit.MILLISECONDS);
                assertThat(m, is(nullValue()));
                return null;
            }

            @Override
            protected void handleLifecycleMessage(LifecycleMessage m) {
                super.handleLifecycleMessage(m);
                handlerCalled.set(true);
            }
        });

        actor2.watch(actor1);

        actor1.join();
        actor2.join();

        assertThat(handlerCalled.get(), is(true));
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
