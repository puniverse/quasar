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
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author pron
 */
public class AsyncListenableFutureTest {
    private FiberScheduler scheduler;

    public AsyncListenableFutureTest() {
        scheduler = new FiberScheduler("test", 4, null, false);
    }

    @Test
    public void simpleTest1() throws Exception {
        final SettableFuture<String> fut = SettableFuture.create();

        final Fiber<String> fiber = new Fiber<>(scheduler, new SuspendableCallable<String>() {
            @Override
            public String run() throws SuspendExecution, InterruptedException {
                try {
                    return AsyncListenableFuture.get(fut);
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

        final Fiber<String> fiber = new Fiber<>(scheduler, new SuspendableCallable<String>() {
            @Override
            public String run() throws SuspendExecution, InterruptedException {
                try {
                    String res = AsyncListenableFuture.get(fut);
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
