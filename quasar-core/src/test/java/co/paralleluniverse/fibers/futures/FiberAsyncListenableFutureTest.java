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
package co.paralleluniverse.fibers.futures;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import jsr166e.ForkJoinPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Ignore;

/**
 *
 * @author pron
 */
public class FiberAsyncListenableFutureTest {
    private ForkJoinPool fjPool;

    public FiberAsyncListenableFutureTest() {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    @Test
    public void simpleTest1() throws Exception {
        final SettableFuture<String> fut = SettableFuture.create();

        final Fiber<String> fiber = new Fiber<>(fjPool, new SuspendableCallable<String>() {
            @Override
            public String run() throws SuspendExecution, InterruptedException {
                try {
                    return FiberAsyncListenableFuture.get(fut);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    fut.set("hi!");
                } catch (InterruptedException e) {
                }
            }
        }).start();

        assertThat(fiber.get(), equalTo("hi!"));
        
    }

    @Test
    public void testException() throws Exception {
        final SettableFuture<String> fut = SettableFuture.create();

        final Fiber<String> fiber = new Fiber<>(fjPool, new SuspendableCallable<String>() {
            @Override
            public String run() throws SuspendExecution, InterruptedException {
                try {
                    String res = FiberAsyncListenableFuture.get(fut);
                    fail();
                    return res;
                } catch (ExecutionException e) {
                    throw Exceptions.rethrow(e.getCause());
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    fut.setException(new RuntimeException("haha!"));
                } catch (InterruptedException e) {
                }
            }
        }).start();

        try {
            fiber.get();
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage(), equalTo("haha!"));
        }
    }
}
