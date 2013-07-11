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
import co.paralleluniverse.actors.ActorSpec;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.actors.behaviors.LocalSupervisor.RestartStrategy;
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
    private static final Logger LOG = LoggerFactory.getLogger(LocalSupervisor.class);
    static final int mailboxSize = 10;
    private static ForkJoinPool fjPool;

    public SupervisorTest() throws Exception {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        java.util.logging.LogManager.getLogManager().readConfiguration(); // gradle messes with the configurations
    }

    private static <T extends LocalActor<Message, V>, Message, V> T spawnActor(T actor) {
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

    private <Message, V> LocalActor<Message, V> getRegisteredActor(String name, long timeout) throws InterruptedException {
        LocalActor<Message, V> a;
        final long start = System.nanoTime();
        while ((a = (LocalActor)LocalActor.getActor(name)) == null || a.isDone()) {
            if (System.nanoTime() > start + TimeUnit.MILLISECONDS.toNanos(timeout))
                return null;
            Thread.sleep(10);
        }
        return a;
    }

    private <Message, V> LocalActor<Message, V> getChild(LocalSupervisor sup, String name, long timeout) throws InterruptedException {
        LocalActor<Message, V> a;
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
        final LocalSupervisor sup = spawnActor(new LocalSupervisor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))));

        LocalActor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(a.get(), is(3));

        sup.shutdown();
        sup.join();
    }

    @Test
    public void whenChildDiesThenRestart() throws Exception {
        final LocalSupervisor sup = spawnActor(new LocalSupervisor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))));

        LocalActor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(a.get(), is(3));


        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 5; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(a.get(), is(5));


        sup.shutdown();
        sup.join();
    }

    @Test
    public void whenChildDiesTooManyTimesThenGiveUpAndDie() throws Exception {
        final LocalSupervisor sup = spawnActor(new LocalSupervisor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 3, 1, TimeUnit.SECONDS, 3, ActorSpec.of(BadActor1.class, "actor1"))));

        LocalActor<Object, Integer> a, prevA = null;

        for (int k = 0; k < 4; k++) {
            a = getChild(sup, "actor1", 1000);
            assertThat(a, not(prevA));

            a.send(1);

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
        final LocalSupervisor sup = spawnActor(new LocalSupervisor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TEMPORARY, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))));

        LocalActor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(a.get(), is(3));


        a = getChild(sup, "actor1", 200);
        assertThat(a, nullValue());


        sup.shutdown();
        sup.join();
    }

    @Test
    public void dontRestartTemporaryChildDeadOfUnnaturalCause() throws Exception {
        final LocalSupervisor sup = spawnActor(new LocalSupervisor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TEMPORARY, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(BadActor1.class, "actor1"))));

        LocalActor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        try {
            a.join();
            fail();
        } catch (ExecutionException e) {
        }

        a = getChild(sup, "actor1", 200);
        assertThat(a, nullValue());

        sup.shutdown();
        sup.join();
    }

    @Test
    public void dontRestartTransientChildDeadOfNaturalCause() throws Exception {
        final LocalSupervisor sup = spawnActor(new LocalSupervisor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TRANSIENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor1.class, "actor1"))));

        LocalActor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(a.get(), is(3));


        a = getChild(sup, "actor1", 200);
        assertThat(a, nullValue());


        sup.shutdown();
        sup.join();
    }

    @Test
    public void restartTransientChildDeadOfUnnaturalCause() throws Exception {
        final LocalSupervisor sup = spawnActor(new LocalSupervisor(RestartStrategy.ONE_FOR_ONE,
                new ChildSpec("actor1", ChildMode.TRANSIENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(BadActor1.class, "actor1"))));

        LocalActor<Object, Integer> a;

        a = getChild(sup, "actor1", 1000);
        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        try {
            a.join();
            fail();
        } catch (ExecutionException e) {
        }


        LocalActor<Object, Integer> b = getChild(sup, "actor1", 200);
        assertThat(b, not(nullValue()));
        assertThat(b, not(a));

        sup.shutdown();
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
        protected LocalActor<Object, Integer> reinstantiate() {
            return new Actor2(getName());
        }
    }

    @Test
    public void restartPreInstantiatedChild() throws Exception {
        final LocalSupervisor sup = spawnActor(new LocalSupervisor(RestartStrategy.ONE_FOR_ONE));

        final LocalActor a1 = new Actor2("actor1");
        sup.addChild(new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, a1));
        LocalActor<Object, Integer> a;

        a = getChild(sup, "actor1", 1);

        assertThat(a, is(a1));

        for (int i = 0; i < 3; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(a.get(), is(3));

        a = getChild(sup, "actor1", 1000);

        assertThat(a, is(not(a1)));

        for (int i = 0; i < 5; i++)
            a.send(1);
        a.send(new ShutdownMessage(null));
        assertThat(a.get(), is(5));

        sup.shutdown();
        sup.join();
    }

    ///////////////// Complex example ///////////////////////////////////////////
    private static class Actor3 extends BasicActor<Integer, Integer> {
        private final LocalSupervisor mySup;
        private final AtomicInteger started;
        private final AtomicInteger terminated;

        public Actor3(String name, AtomicInteger started, AtomicInteger terminated) {
            super(name);
            mySup = (LocalSupervisor) LocalActor.self();
            this.started = started;
            this.terminated = terminated;
        }

        @Override
        protected Integer doRun() throws SuspendExecution, InterruptedException {
            final LocalGenServer<Message1, Integer, Void> adder = spawnActor(new LocalGenServer<Message1, Integer, Void>() {
                @Override
                protected void init() throws SuspendExecution {
                    started.incrementAndGet();
                }

                @Override
                protected void terminate(Throwable cause) throws SuspendExecution {
                    terminated.incrementAndGet();
                }

                @Override
                protected Integer handleCall(Actor<Integer> from, Object id, Message1 m) throws Exception, SuspendExecution {
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

            int res = adder.call(new Message1(a, b));
            return res;
        }

        @Override
        protected LocalActor<Integer, Integer> reinstantiate() {
            return new Actor3(getName(), started, terminated);
        }
    }

    @Test
    public void testComplex1() throws Exception {
        AtomicInteger started = new AtomicInteger();
        AtomicInteger terminated = new AtomicInteger();

        final LocalSupervisor sup = spawnActor(new LocalSupervisor(RestartStrategy.ALL_FOR_ONE,
                new ChildSpec("actor1", ChildMode.PERMANENT, 5, 1, TimeUnit.SECONDS, 3, ActorSpec.of(Actor3.class, "actor1", started, terminated))));

        LocalActor<Integer, Integer> a;

        a = getChild(sup, "actor1", 1000);
        a.send(3);
        a.send(4);

        assertThat(a.get(), is(7));

        a = getChild(sup, "actor1", 1000);
        a.send(70);
        a.send(80);

        try {
            a.get();
            fail();
        } catch (ExecutionException e) {
        }

        a = getChild(sup, "actor1", 1000);
        a.send(7);
        a.send(8);

        assertThat(a.get(), is(15));

        Thread.sleep(100); // give the actor time to start the GenServer

        sup.shutdown();
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
