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
import co.paralleluniverse.common.util.Debug;
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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;

/**
 * These tests are also good tests for sendSync, as they test sendSync (and receive) from both fibers and threads.
 *
 * @author pron
 */
public class GenEventTest {
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
    static final int mailboxSize = 10;
    private ForkJoinPool fjPool;

    public GenEventTest() {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    private LocalGenEvent<String> spawnGenEvent(Initializer initializer) {
        return spawnActor(new LocalGenEvent<String>("genevent", initializer));
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
    public void testInitializationAndTermination() throws Exception {
        final Initializer init = mock(Initializer.class);
        LocalGenEvent<String> ge = spawnGenEvent(init);

        verify(init).init();

        ge.shutdown();
        ge.join(100, TimeUnit.MILLISECONDS);

        verify(init).terminate(null);
    }

    @Test
    public void testNotify() throws Exception {
        final EventHandler<String> handler1 = mock(EventHandler.class);
        final EventHandler<String> handler2 = mock(EventHandler.class);

        final LocalGenEvent<String> ge = spawnGenEvent(null);

        ge.addHandler(handler1);
        ge.addHandler(handler2);

        ge.notify("hello");

        Thread.sleep(100);
        InOrder inOrder = inOrder(handler1, handler2);
        inOrder.verify(handler1).handleEvent("hello");
        inOrder.verify(handler2).handleEvent("hello");

        ge.removeHandler(handler1);

        ge.notify("goodbye");

        ge.shutdown();
        ge.join(100, TimeUnit.MILLISECONDS);

        verify(handler1, never()).handleEvent("goodbye");
        verify(handler2).handleEvent("goodbye");
    }

    @Ignore
    @Test
    public void testExceptionThrownInHandler() throws Exception {
        final Initializer init = mock(Initializer.class);
        final EventHandler<String> handler1 = mock(EventHandler.class);
        final EventHandler<String> handler2 = mock(EventHandler.class);

        final Exception myException = new RuntimeException("haha!");
        doThrow(myException).when(handler1).handleEvent(anyString());

        final LocalGenEvent<String> ge = spawnGenEvent(init);

        ge.addHandler(handler1);
        ge.addHandler(handler2);

        ge.notify("hello");

        verify(handler1).handleEvent("hello");
        verify(handler2, never()).handleEvent(anyString());

        verify(init).terminate(myException);

        ge.join(100, TimeUnit.MILLISECONDS);
    }
}
