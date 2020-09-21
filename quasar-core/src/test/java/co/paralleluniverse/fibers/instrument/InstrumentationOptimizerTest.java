/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Instrumented;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author circlespainter
 */
public class InstrumentationOptimizerTest {

    private boolean isOptimized(String method) {
        for (Method m : getClass().getDeclaredMethods()) {
            if (method.equals(m.getName())) {
                Instrumented i = m.getAnnotation(Instrumented.class);
                return i != null && i.methodOptimized();
            }
        }
        return false;
    }

    public void sleepFiberVoid() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
    }

    private void skipForwardsToSuspendableVoid() throws InterruptedException, SuspendExecution {
        sleepFiberVoid();
    }

    @Test
    public void testSkipForwardsToSuspendableVoid() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                skipForwardsToSuspendableVoid();
            }
        }).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableVoid"));
    }

    private Object sleepFiberObject() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        return new Object();
    }

    private Object skipForwardsToSuspendableObject() throws InterruptedException, SuspendExecution {
        return sleepFiberObject();
    }

    @Test
    public void testSkipForwardsToSuspendableObject() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                skipForwardsToSuspendableObject();
            }
        }).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableObject"));
    }

    private double sleepFiberDouble() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextDouble();
    }

    private double skipForwardsToSuspendableDouble() throws InterruptedException, SuspendExecution {
        return sleepFiberDouble();
    }

    @Test
    public void testSkipForwardsToSuspendableDouble() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                skipForwardsToSuspendableDouble();
            }
        }).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableDouble"));
    }

    private float sleepFiberFloat() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextFloat();
    }

    private float skipForwardsToSuspendableFloat() throws InterruptedException, SuspendExecution {
        return sleepFiberFloat();
    }

    @Test
    public void testSkipForwardsToSuspendableFloat() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                skipForwardsToSuspendableFloat();
            }
        }).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableFloat"));
    }

    private int sleepFiberInt() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextInt();
    }

    private int skipForwardsToSuspendableInt() throws InterruptedException, SuspendExecution {
        return sleepFiberInt();
    }

    @Test
    public void testSkipForwardsToSuspendableInt() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                skipForwardsToSuspendableInt();
            }
        }).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableInt"));
    }

    private long sleepFiberLong() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextLong();
    }

    private long skipForwardsToSuspendableLong() throws InterruptedException, SuspendExecution {
        return sleepFiberLong();
    }

    @Test
    public void testSkipForwardsToSuspendableLong() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                skipForwardsToSuspendableLong();
            }
        }).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableLong"));
    }

    @Suspendable
    private Object dontSkipForwardsWithTryCatch() throws InterruptedException {
        try {
            return sleepFiberObject();
        } catch (SuspendExecution e) {
            return null;
        }
    }

    @Test
    public void testDontSkipForwardsWithTryCatch() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                dontSkipForwardsWithTryCatch();
            }
        }).start().join();
        assertFalse(isOptimized("skipForwardsWithTryCatch"));
    }

    private void dontSkipForwardsWithLoop() throws InterruptedException, SuspendExecution {
        for (int i = 0; i < 3; i++)
            sleepFiberVoid();
    }

    @Test
    public void testDontSkipForwardsWithLoop() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                dontSkipForwardsWithLoop();
            }
        }).start().join();
        assertFalse(isOptimized("skipForwardsWithLoop"));
    }

    private void dontSkipForwardsWithLoopBefore() throws InterruptedException, SuspendExecution {
        for (int i = 0; i < 5; i++)
            i++;
        sleepFiberVoid();
    }

    @Test
    public void testDontSkipForwardsWithLoopBefore() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                dontSkipForwardsWithLoopBefore();
            }
        }).start().join();
        assertFalse(isOptimized("skipForwardsWithLoopBefore"));
    }

    private void skipForwardsWithLoopAfter() throws InterruptedException, SuspendExecution {
        sleepFiberVoid();
        for (int i = 0; i < 3; i++)
            System.nanoTime();
    }

    @Test
    public void testSkipForwardsWithLoopAfter() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                skipForwardsWithLoopAfter();
            }
        }).start().join();
        assertTrue(isOptimized("skipForwardsWithLoopAfter"));
    }

    private void dontSkipForwardsWithMethodBefore() throws InterruptedException, SuspendExecution {
        System.nanoTime();
        sleepFiberVoid();
    }

    @Test
    public void testDontSkipForwardsWithMethodBefore() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                dontSkipForwardsWithMethodBefore();
            }
        }).start().join();
        assertFalse(isOptimized("skipForwardsWithMethodBefore"));
    }

    private void skipForwardsWithMethodAfter() throws InterruptedException, SuspendExecution {
        sleepFiberVoid();
        System.nanoTime();
    }

    @Test
    public void testSkipForwardsWithMethodAfter() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                skipForwardsWithMethodAfter();
            }
        }).start().join();
        assertTrue(isOptimized("skipForwardsWithMethodAfter"));
    }

    private void dontSkipForwardsWithReflectiveCalls() throws InterruptedException, SuspendExecution, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        sleepFiberVoid();
        getClass().getMethod("sleepFiberVoid").invoke(this);
    }

    @Test
    public void testDontSkipForwardsWithReflectiveCalls() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber<>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    dontSkipForwardsWithReflectiveCalls();
                } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }).start().join();
        assertFalse(isOptimized("skipForwardsWithReflectiveCalls"));
    }
}
