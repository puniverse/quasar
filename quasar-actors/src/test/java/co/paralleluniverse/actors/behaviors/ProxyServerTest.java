/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRegistry;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channels;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Ignore;
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
public class ProxyServerTest {
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

    @After
    public void tearDown() {
        ActorRegistry.clear();
    }
    
    static final MailboxConfig mailboxConfig = new MailboxConfig(10, Channels.OverflowPolicy.THROW);
    private FiberScheduler scheduler;

    public ProxyServerTest() {
        scheduler = new FiberForkJoinScheduler("test", 4, null, false);
    }

    private Server<?, ?, ?> spawnServer(boolean callOnVoidMethods, Object target) {
        return new ProxyServerActor("server", callOnVoidMethods, target).spawn(scheduler);
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

    @Suspendable
    public static interface A {
        int foo(String str, int x); // throws SuspendExecution;

        void bar(int x); // throws SuspendExecution;
    }

    @Test
    public void testShutdown() throws Exception {
        final Server<?, ?, ?> a = spawnServer(false, new A() {
            public int foo(String str, int x) {
                return str.length() + x;
            }

            public void bar(int x) {
                throw new UnsupportedOperationException();
            }
        });

        a.shutdown();
        LocalActor.join(a, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledThenResultIsReturned() throws Exception {
        final Server<?, ?, ?> a = spawnServer(false, new A() {
            @Suspendable
            public int foo(String str, int x) {
                try {
                    Strand.sleep(50);
                    return str.length() + x;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }

            public void bar(int x) {
                throw new UnsupportedOperationException();
            }
        });

        Actor<?, Integer> actor = spawnActor(new BasicActor<Object, Integer>(mailboxConfig) {
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                return ((A) a).foo("hello", 2);
            }
        });

        int res = actor.get();
        assertThat(res, is(7));

        a.shutdown();
        LocalActor.join(a, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledFromThreadThenResultIsReturned() throws Exception {
        final Server<?, ?, ?> a = spawnServer(false, new A() {
            @Suspendable
            public int foo(String str, int x) {
                try {
                    Strand.sleep(50);
                    return str.length() + x;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }

            public void bar(int x) {
                throw new UnsupportedOperationException();
            }
        });

        int res = ((A) a).foo("hello", 2);
        assertThat(res, is(7));

        ((A) a).bar(3);

        res = ((A) a).foo("hello", 2);
        assertThat(res, is(7));

        a.shutdown();
        LocalActor.join(a, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenHandleCallThrowsExceptionThenItPropagatesToCaller() throws Exception {
        final Server<?, ?, ?> a = spawnServer(false, new A() {
            public int foo(String str, int x) {
                throw new RuntimeException("my exception");
            }

            public void bar(int x) {
                throw new UnsupportedOperationException();
            }
        });

        Actor<?, Void> actor = spawnActor(new BasicActor<Object, Void>(mailboxConfig) {
            protected Void doRun() throws SuspendExecution, InterruptedException {
                try {
                    int res = ((A) a).foo("hello", 2);
                    fail();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    assertThat(e.getMessage(), equalTo("my exception"));
                }
                return null;
            }
        });

        actor.join();
        a.shutdown();
        LocalActor.join(a, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenHandleCallThrowsExceptionThenItPropagatesToThreadCaller() throws Exception {
        final Server<?, ?, ?> a = spawnServer(false, new A() {
            public int foo(String str, int x) {
                throw new RuntimeException("my exception");
            }

            public void bar(int x) {
                throw new UnsupportedOperationException();
            }
        });

        try {
            int res = ((A) a).foo("hello", 2);
            fail();
        } catch (RuntimeException e) {
            e.printStackTrace();
            assertThat(e.getMessage(), equalTo("my exception"));
        }

        a.shutdown();
        LocalActor.join(a, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testCast() throws Exception {
        final AtomicInteger called = new AtomicInteger(0);
        final Server<?, ?, ?> a = spawnServer(false, new A() {
            public int foo(String str, int x) {
                throw new UnsupportedOperationException();
            }

            @Suspendable
            public void bar(int x) {
                try {
                    Strand.sleep(100);
                    called.set(x);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }
        });

        ((A) a).bar(15);
        assertThat(called.get(), is(0));
        Thread.sleep(200);
        assertThat(called.get(), is(15));

        a.shutdown();
        LocalActor.join(a, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testCallOnVoidMethod() throws Exception {
        final AtomicInteger called = new AtomicInteger(0);
        final Server<?, ?, ?> a = spawnServer(true, new A() {
            public int foo(String str, int x) {
                throw new UnsupportedOperationException();
            }

            @Suspendable
            public void bar(int x) {
                try {
                    Strand.sleep(100);
                    called.set(x);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }
        });

        ((A) a).bar(15);
        assertThat(called.get(), is(15));

        a.shutdown();
        LocalActor.join(a, 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void whenCalledAndTimeoutThenThrowTimeout() throws Exception {
        final Server<?, ?, ?> a = spawnServer(false, new A() {
            @Suspendable
            public int foo(String str, int x) {
                try {
                    Strand.sleep(100);
                    return str.length() + x;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }

            public void bar(int x) {
                throw new UnsupportedOperationException();
            }
        });

        a.setDefaultTimeout(50, TimeUnit.MILLISECONDS);

        try {
            int res = ((A) a).foo("hello", 2);
            fail("res: " + res);
        } catch (RuntimeException e) {
            assertThat(e.getCause(), instanceOf(TimeoutException.class));
        }

        a.setDefaultTimeout(200, TimeUnit.MILLISECONDS);

        int res = ((A) a).foo("hello", 2);
        assertThat(res, is(7));

        a.shutdown();
        LocalActor.join(a, 500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRegistration() throws Exception {
        final Server<?, ?, ?> a = new ProxyServerActor("test1", false, new A() {
            public int foo(String str, int x) {
                throw new UnsupportedOperationException();
            }

            @Suspendable
            public void bar(int x) {
            }
        }) {

            @Override
            protected void init() throws InterruptedException, SuspendExecution {
                register();
            }

        }.spawn();

        assertTrue((A) a == (A) ActorRegistry.getActor("test1"));
    }
}
