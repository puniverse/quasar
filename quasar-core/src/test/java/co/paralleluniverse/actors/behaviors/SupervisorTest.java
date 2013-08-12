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
import co.paralleluniverse.actors.ActorSpec;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.actors.behaviors.SupervisorActor.RestartStrategy;
import co.paralleluniverse.actors.behaviors.Supervisor.ChildMode;
import co.paralleluniverse.actors.behaviors.Supervisor.ChildSpec;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jsr166e.ForkJoinPool;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
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
    private static final Logger LOG = LoggerFactory.getLogger(SupervisorActor.class);
    static final int mailboxSize = 10;
    private static ForkJoinPool fjPool;

    public SupervisorTest() throws Exception {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        java.util.logging.LogManager.getLogManager().readConfiguration(); // gradle messes with the configurations
    }

    private static <T extends Actor<Message, V>, Message, V> T spawnActor(T actor) {
        Fiber fiber = new Fiber(fjPool, actor);
        fiber.start();
        return actor;
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
        protected void handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ShutdownMessage)
                Strand.currentStrand().interrupt();
            else
                super.handleLifecycleMessage(m);
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

    private <Message, V> Actor<Message, V> getChild(SupervisorActor sup, String name, long timeout) throws InterruptedException {
        Actor<Message, V> a;
        final long start = System.nanoTime();
        while ((a = sup.getChild(name)) == null || a.isDone()) {
            if (System.nanoTime() > start + TimeUnit.MILLISECONDS.toNanos(timeout))
                return null;
            Thread.sleep(10);
        }
        return a;
    }

    @Test
    public void startChild() throws Exception {
        final SupervisorActor sup = spawnActor(new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))));

        Actor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.ref().send(1);
        a.ref().send(new ShutdownMessage(null));
        assertThat(a.get(), is(3));

        sup.ref().shutdown();
        sup.join();
    }

    @Test
    public void whenChildDiesThenRestart() throws Exception {
        final SupervisorActor sup = spawnActor(new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))));

        Actor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.ref().send(1);
        a.ref().send(new ShutdownMessage(null));
        assertThat(a.get(), is(3));


        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 5; i++)
            a.ref().send(1);
        a.ref().send(new ShutdownMessage(null));
        assertThat(a.get(), is(5));


        sup.ref().shutdown();
        sup.join();
    }

    @Test
    public void whenChildDiesTooManyTimesThenGiveUpAndDie() throws Exception {
        final SupervisorActor sup = spawnActor(new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 3, 1, TimeUnit.SECONDS, 3, ActorSpec.of(BadActor1.class, "actor1"))));

        Actor<Object, Integer> a, prevA = null;

        for (int k = 0; k < 4; k++) {
            a = getChild(sup, "actor1", 1000);
            assertThat(a, not(prevA));

            a.ref().send(1);

            try {
                a.join();
                fail();
            } catch (ExecutionException e) {
            }

            prevA = a;
        }

        sup.join(20, TimeUnit.MILLISECONDS);
    }

    @Test
    public void dontRestartTemporaryChildDeadOfNaturalCause() throws Exception {
        final SupervisorActor sup = spawnActor(new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TEMPORARY, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))));

        Actor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.ref().send(1);
        a.ref().send(new ShutdownMessage(null));
        assertThat(a.get(), is(3));


        a = getChild(sup, "actor1", 200);
        assertThat(a, nullValue());


        sup.ref().shutdown();
        sup.join();
    }

    @Test
    public void dontRestartTemporaryChildDeadOfUnnaturalCause() throws Exception {
        final SupervisorActor sup = spawnActor(new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TEMPORARY, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(BadActor1.class, "actor1"))));

        Actor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.ref().send(1);
        a.ref().send(new ShutdownMessage(null));
        try {
            a.join();
            fail();
        } catch (ExecutionException e) {
        }

        a = getChild(sup, "actor1", 200);
        assertThat(a, nullValue());

        sup.ref().shutdown();
        sup.join();
    }

    @Test
    public void dontRestartTransientChildDeadOfNaturalCause() throws Exception {
        final SupervisorActor sup = spawnActor(new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TRANSIENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))));

        Actor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.ref().send(1);
        a.ref().send(new ShutdownMessage(null));
        assertThat(a.get(), is(3));


        a = getChild(sup, "actor1", 200);
        assertThat(a, nullValue());


        sup.ref().shutdown();
        sup.join();
    }

    @Test
    public void restartTransientChildDeadOfUnnaturalCause() throws Exception {
        final SupervisorActor sup = spawnActor(new SupervisorActor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TRANSIENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(BadActor1.class, "actor1"))));

        Actor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.ref().send(1);
        a.ref().send(new ShutdownMessage(null));
        try {
            a.join();
            fail();
        } catch (ExecutionException e) {
        }


        Actor<Object, Integer> b = getChild(sup, "actor1", 200);
        assertThat(b, not(nullValue()));
        assertThat(b, not(a));

        sup.ref().shutdown();
        sup.join();
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
        protected void handleLifecycleMessage(LifecycleMessage m) {
            if (m instanceof ShutdownMessage)
                Strand.currentStrand().interrupt();
            else
                super.handleLifecycleMessage(m);
        }

        @Override
        protected Actor<Object, Integer> reinstantiate() {
            return new Actor2(getName());
        }
    }

    @Test
    public void restartPreInstantiatedChild() throws Exception {
        final SupervisorActor sup = spawnActor(new SupervisorActor(RestartStrategy.ONE_FOR_ONE));

        final Actor a1 = new Actor2("actor1");
        sup.addChild(new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, a1));
        Actor<Object, Integer> a;

        a = getChild(sup, "actor1", 1);

        assertThat(a, is(a1));

        for (int i = 0; i < 3; i++)
            a.ref().send(1);
        a.ref().send(new ShutdownMessage(null));
        assertThat(a.get(), is(3));

        a = getChild(sup, "actor1", 1000);

        assertThat(a, is(not(a1)));

        for (int i = 0; i < 5; i++)
            a.ref().send(1);
        a.ref().send(new ShutdownMessage(null));
        assertThat(a.get(), is(5));

        sup.ref().shutdown();
        sup.join();
    }

    ///////////////// Complex example ///////////////////////////////////////////
    private static class Actor3 extends BasicActor<Integer, Integer> {
        private final SupervisorActor mySup;
        private final AtomicInteger started;
        private final AtomicInteger terminated;

        public Actor3(String name, AtomicInteger started, AtomicInteger terminated) {
            super(name);
            mySup = (SupervisorActor) Actor.self();
            this.started = started;
            this.terminated = terminated;
        }

        @Override
        protected Integer doRun() throws SuspendExecution, InterruptedException {
            final GenServerActor<Message1, Integer, Void> adder = spawnActor(new GenServerActor<Message1, Integer, Void>() {
                @Override
                protected void init() throws SuspendExecution {
                    started.incrementAndGet();
                }

                @Override
                protected void terminate(Throwable cause) throws SuspendExecution {
                    terminated.incrementAndGet();
                }

                @Override
                protected Integer handleCall(ActorRef<Integer> from, Object id, Message1 m) throws Exception, SuspendExecution {
                    int res = m.a + m.b;
                    if (res > 100)
                        throw new RuntimeException("oops!");
                    return res;
                }
            });

            //link(adder);
            mySup.addChild(new ChildSpec(null, ChildMode.TEMPORARY, 10, 1, TimeUnit.SECONDS, 1, adder));

            int a = receive();
            int b = receive();

            int res = adder.ref().call(new Message1(a, b));
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

        final SupervisorActor sup = spawnActor(new SupervisorActor(RestartStrategy.ALL_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor3.class, "actor1", started, terminated))));

        Actor<Integer, Integer> a;

        a = getChild(sup, "actor1", 1000);
        a.ref().send(3);
        a.ref().send(4);

        assertThat(a.get(), is(7));

        a = getChild(sup, "actor1", 1000);
        a.ref().send(70);
        a.ref().send(80);

        try {
            a.get();
            fail();
        } catch (ExecutionException e) {
        }

        a = getChild(sup, "actor1", 1000);
        a.ref().send(7);
        a.ref().send(8);

        assertThat(a.get(), is(15));

        Thread.sleep(100); // give the actor time to start the GenServer

        sup.ref().shutdown();
        sup.join();

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
