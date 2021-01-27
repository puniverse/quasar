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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorRegistry;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.MessageProcessor;
import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;

/**
 *
 * @author pron
 */
public class FiniteStateMachineTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;

    @After
    public void tearDown() {
        ActorRegistry.clear();
    }

    static final int mailboxSize = 10;

    public FiniteStateMachineTest() {
    }

    private <T extends Actor<Message, V>, Message, V> T spawnActor(T actor) {
        Fiber fiber = new Fiber(actor);
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
    public void testInitializationAndTermination() throws Exception {
        final Initializer init = mock(Initializer.class);
        ActorRef<Object> a = new FiniteStateMachineActor(init).spawn();

        Thread.sleep(100);
        verify(init).init();

        LocalActor.join(a, 100, TimeUnit.MILLISECONDS);

        verify(init).terminate(null);
    }

    @Test
    public void testStates() throws Exception {
        final AtomicBoolean success = new AtomicBoolean();

        ActorRef<Object> a = new FiniteStateMachineActor() {
            @Override
            protected SuspendableCallable<SuspendableCallable> initialState() {
                return new SuspendableCallable<SuspendableCallable>() {
                    public SuspendableCallable run() throws SuspendExecution, InterruptedException {
                        return state1();
                    }
                };
            }

            private SuspendableCallable<SuspendableCallable> state1() throws SuspendExecution, InterruptedException {
                return receive(new MessageProcessor<Object, SuspendableCallable<SuspendableCallable>>() {
                    @Override
                    public SuspendableCallable<SuspendableCallable> process(Object m) throws SuspendExecution, InterruptedException {
                        if ("a".equals(m))
                            return new SuspendableCallable<SuspendableCallable>() {
                                public SuspendableCallable run() throws SuspendExecution, InterruptedException {
                                    return state2();
                                }
                            };
                        return null;
                    }
                });

            }

            private SuspendableCallable<SuspendableCallable> state2() throws SuspendExecution, InterruptedException {
                return receive(new MessageProcessor<Object, SuspendableCallable<SuspendableCallable>>() {
                    @Override
                    public SuspendableCallable<SuspendableCallable> process(Object m) throws SuspendExecution, InterruptedException {
                        if ("b".equals(m)) {
                            success.set(true);
                            return TERMINATE;
                        }
                        return null;
                    }
                });
            }
        }.spawn();

        a.send("b");
        a.send("a");

        LocalActor.join(a, 100, TimeUnit.MILLISECONDS);

        assertTrue(success.get());
    }

    @Ignore
    @Test
    public void testExceptionThrownInState() throws Exception {
        final Initializer init = mock(Initializer.class);
        final RuntimeException myException = new RuntimeException("haha!");

        ActorRef<Object> a = new FiniteStateMachineActor() {
            @Override
            protected SuspendableCallable<SuspendableCallable> initialState() {
                return new SuspendableCallable<SuspendableCallable>() {
                    public SuspendableCallable run() throws SuspendExecution, InterruptedException {
                        return state1();
                    }
                };
            }

            private SuspendableCallable<SuspendableCallable> state1() {
                return new SuspendableCallable<SuspendableCallable>() {
                    public SuspendableCallable run() throws SuspendExecution, InterruptedException {
                        return state2();
                    }
                };
            }

            private SuspendableCallable<SuspendableCallable> state2() {
                throw myException;
            }
        }.spawn();

        LocalActor.join(a, 100, TimeUnit.MILLISECONDS);

        verify(init).terminate(myException);
    }
}
