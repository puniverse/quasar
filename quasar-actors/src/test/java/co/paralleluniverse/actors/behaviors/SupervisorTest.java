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
import co.paralleluniverse.actors.ActorSpec;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.actors.behaviors.Supervisor.ChildMode;
import co.paralleluniverse.actors.behaviors.Supervisor.ChildSpec;
import co.paralleluniverse.actors.behaviors.SupervisorActor.RestartStrategy;
import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * These tests are also good tests for sendSync, as they test sendSync (and receive) from both fibers and threads.
 *
 * @author pron
 */
public class SupervisorTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;
    
    @After
    public void tearDown() {
        ActorRegistry.clear();
        scheduler.shutdown();
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(SupervisorActor.class);
    static final int mailboxSize = 10;
    private static FiberScheduler scheduler;
    private static FiberFactory factory;

    public SupervisorTest() throws Exception {
        factory = scheduler = new FiberForkJoinScheduler("test", 4, null, false);
//        java.util.logging.LogManager.getLogManager().readConfiguration(); // gradle messes with the configurations
    }

    private static class Actor1 extends BasicActor<Object, Integer> {
        public Actor1(String name) {
            super(name);
        }

        @Override
        protected Integer doRun() throws SuspendExecution, InterruptedException {
            register();
            int i = 0;
            try {
                for (;;) {
                    Object m = receive();
                    i++;
                }
            } catch (InterruptedException e) {
                return i;
            }
        }

        @Override
        protected Object handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ShutdownMessage)
                Strand.currentStrand().interrupt();
            else
                super.handleLifecycleMessage(m);
            return null;
        }
    }

    private static class BadActor1 extends BasicActor<Object, Integer> {
        public BadActor1(String name) {
            super(name);
        }

        @Override
        protected Integer doRun() throws SuspendExecution, InterruptedException {
            register();
            int i = 0;
            try {
                for (;;) {
                    Object m = receive();
                    i++;
                    throw new RuntimeException("Ha!");
                }
            } catch (InterruptedException e) {
                return i;
            }
        }
    }

//    private <Message, V> Actor<Message, V> getRegisteredActor(String name, long timeout) throws InterruptedException {
//        Actor<Message, V> a;
//        final long start = System.nanoTime();
//        while ((a = (Actor)Actor.getActor(name)) == null || a.isDone()) {
//            if (System.nanoTime() > start + TimeUnit.MILLISECONDS.toNanos(timeout))
//                return null;
//            Thread.sleep(10);
//        }
//        return a;
//    }
    private <Message> ActorRef<Message> getChild(Supervisor sup, String name, long timeout) throws InterruptedException, SuspendExecution {
        return (ActorRef<Message>) sup.getChild(name);
//        Actor<Message, V> a;
//        final long start = System.nanoTime();
//        while ((a = sup.getChild(name)) == null || a.isDone()) {
//            if (System.nanoTime() > start + TimeUnit.MILLISECONDS.toNanos(timeout))
//                return null;
//            Thread.sleep(10);
//        }
//        return a;
    }

    private <Message> List<ActorRef<Message>> getChildren(final Supervisor sup) throws InterruptedException, SuspendExecution {
        return (List<ActorRef<Message>>) sup.getChildren();
    }

    @Test
    public void startChild() throws Exception {
        final Supervisor sup = new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))).spawn(factory);

        ActorRef<Object> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(LocalActor.<Integer>get(a), is(3));

        sup.shutdown();
        LocalActor.join(sup);
    }

    @Test
    public void whenChildDiesThenRestart() throws Exception {
        final Supervisor sup = new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))).spawn(factory);

        ActorRef<Object> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(LocalActor.<Integer>get(a), is(3));

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 5; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(LocalActor.<Integer>get(a), is(5));

        sup.shutdown();
        LocalActor.join(sup);
    }

    @Test
    public void whenChildDiesTooManyTimesThenGiveUpAndDie() throws Exception {
        final Supervisor sup = new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 3, 1, TimeUnit.SECONDS, 3, ActorSpec.of(BadActor1.class, "actor1"))).spawn();

        ActorRef<Object> a, prevA = null;

        for (int k = 0; k < 4; k++) {
            a = getChildren(sup).get(0);
            assertThat(a, not(prevA));

            a.send(1);

            try {
                LocalActor.join(a);
                fail();
            } catch (ExecutionException e) {
            }

            prevA = a;
        }

        LocalActor.join(sup, 20, TimeUnit.MILLISECONDS);
    }

    @Test
    public void dontRestartTemporaryChildDeadOfNaturalCause() throws Exception {
        final Supervisor sup = new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TEMPORARY, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))).spawn();

        ActorRef<Object> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(LocalActor.<Integer>get(a), is(3));

        a = getChild(sup, "actor1", 200);
        assertThat(a, nullValue());

        List<ActorRef<Object>> cs = getChildren(sup);
        assertEquals(cs.size(), 0);

        sup.shutdown();
        LocalActor.join(sup);
    }

    @Test
    public void dontRestartTemporaryChildDeadOfUnnaturalCause() throws Exception {
        final Supervisor sup = new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TEMPORARY, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(BadActor1.class, "actor1"))).spawn(factory);

        ActorRef<Object> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        try {
            LocalActor.join(a);
            fail();
        } catch (ExecutionException e) {
        }

        a = getChild(sup, "actor1", 200);
        assertThat(a, nullValue());

        sup.shutdown();
        LocalActor.join(sup);
    }

    @Test
    public void dontRestartTransientChildDeadOfNaturalCause() throws Exception {
        final Supervisor sup = new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TRANSIENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))).spawn();

        ActorRef<Object> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(LocalActor.<Integer>get(a), is(3));

        a = getChild(sup, "actor1", 200);
        assertThat(a, nullValue());

        sup.shutdown();
        LocalActor.join(sup);
    }

    @Test
    public void restartTransientChildDeadOfUnnaturalCause() throws Exception {
        final Supervisor sup = new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TRANSIENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(BadActor1.class, "actor1"))).spawn();

        ActorRef<Object> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        try {
            LocalActor.join(a);
            fail();
        } catch (ExecutionException e) {
        }

        ActorRef<Object> b = getChild(sup, "actor1", 200);
        assertThat(b, not(nullValue()));
        assertThat(b, not(equalTo(a)));

        List<ActorRef<Object>> bcs = getChildren(sup);
        assertThat(bcs.get(0), not(nullValue()));
        assertThat(bcs.get(0), not(equalTo(a)));
        assertEquals(bcs.size(), 1);

        sup.shutdown();
        LocalActor.join(sup);
    }

    private static class Actor2 extends BasicActor<Object, Integer> {
        public Actor2(String name) {
            super(name);
        }

        @Override
        protected Integer doRun() throws SuspendExecution, InterruptedException {
            register();
            int i = 0;
            try {
                for (;;) {
                    Object m = receive();
                    i++;
                }
            } catch (InterruptedException e) {
                return i;
            }
        }

        @Override
        protected Object handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ShutdownMessage)
                Strand.currentStrand().interrupt();
            else
                super.handleLifecycleMessage(m);
            return null;
        }

        @Override
        protected Actor<Object, Integer> reinstantiate() {
            return new Actor2(getName());
        }
    }

    @Test
    public void restartPreInstantiatedChild() throws Exception {
        final Supervisor sup = new SupervisorActor(RestartStrategy.ONE_FOR_ONE).spawn();

        final ActorRef<Object> a1 = new Actor2("actor1").spawn();
        sup.addChild(new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, a1));
        ActorRef<Object> a;

        a = getChild(sup, "actor1", 1);

        assertThat(a, equalTo(a1));

        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(LocalActor.<Integer>get(a), is(3));

        a = getChild(sup, "actor1", 1000);

        assertThat(a, is(not(equalTo(a1))));

        for (int i = 0; i < 5; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(LocalActor.<Integer>get(a), is(5));

        sup.shutdown();
        LocalActor.join(sup);
    }

    @Test
    public void testRegistration() throws Exception {
        Supervisor s = new SupervisorActor(RestartStrategy.ONE_FOR_ONE) {
            @Override
            protected void init() throws SuspendExecution, InterruptedException {
                // Strand.sleep(1000);
                register("test1");
            }
        }.spawn();

        assertTrue(s == (Supervisor) ActorRegistry.getActor("test1"));
    }

    ///////////////// Complex example ///////////////////////////////////////////
    private static class Actor3 extends BasicActor<Integer, Integer> {
        private final Supervisor mySup;
        private final AtomicInteger started;
        private final AtomicInteger terminated;

        public Actor3(String name, AtomicInteger started, AtomicInteger terminated) {
            super(name);
            mySup = LocalActor.self();
            this.started = started;
            this.terminated = terminated;
        }

        @Override
        protected Integer doRun() throws SuspendExecution, InterruptedException {
            final Server<Message1, Integer, Void> adder = new ServerActor<Message1, Integer, Void>() {
                @Override
                protected void init() throws SuspendExecution {
                    started.incrementAndGet();
                }

                @Override
                protected void terminate(Throwable cause) throws SuspendExecution {
                    terminated.incrementAndGet();
                }

                @Override
                protected Integer handleCall(ActorRef<?> from, Object id, Message1 m) throws Exception, SuspendExecution {
                    int res = m.a + m.b;
                    if (res > 100)
                        throw new RuntimeException("oops!");
                    return res;
                }
            }.spawn();

            //link(adder);
            mySup.addChild(new ChildSpec(null, ChildMode.TEMPORARY, 10, 1, TimeUnit.SECONDS, 1, adder));

            int a = receive();
            int b = receive();

            int res = adder.call(new Message1(a, b));
            return res;
        }

        @Override
        protected Actor<Integer, Integer> reinstantiate() {
            return new Actor3(getName(), started, terminated);
        }
    }

    @Test
    public void testComplex1() throws Exception {
        AtomicInteger started = new AtomicInteger();
        AtomicInteger terminated = new AtomicInteger();

        final Supervisor sup = new SupervisorActor(RestartStrategy.ALL_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor3.class, "actor1", started, terminated))).spawn();

        ActorRef<Integer> a;

        a = getChild(sup, "actor1", 1000);
        a.send(3);
        a.send(4);

        assertThat(LocalActor.<Integer>get(a), is(7));

        a = getChild(sup, "actor1", 1000);
        a.send(70);
        a.send(80);

        try {
            LocalActor.<Integer>get(a);
            fail();
        } catch (ExecutionException e) {
        }

        a = getChild(sup, "actor1", 1000);
        a.send(7);
        a.send(8);

        assertThat(LocalActor.<Integer>get(a), is(15));

        Thread.sleep(100); // give the actor time to start the GenServer

        sup.shutdown();
        LocalActor.join(sup);

        assertThat(started.get(), is(4));
        assertThat(terminated.get(), is(4));
    }

    static class Message1 {
        final int a;
        final int b;

        public Message1(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }
}
