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
package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Instrumented;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.instrument.InstrumentMethod;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

/**
 *
 * @author circlespainter
 */
public final class InstrumentationOptimizerTest {

    private boolean isOptimized(String method) {
        for (Method m : getClass().getDeclaredMethods()) {
            if (method.equals(m.getName())) {
                Instrumented i = m.getAnnotation(Instrumented.class);
                return i != null && i.isMethodInstrumentationOptimized();
            }
        }
        return false;
    }

    public final void sleepFiberVoid() throws InterruptedException {
        Fiber.sleep(10);
    }

    private void skipForwardsToSuspendableVoid() throws InterruptedException {
        sleepFiberVoid();
    }

    @Test
    public final void testSkipForwardsToSuspendableVoid() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::skipForwardsToSuspendableVoid).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableVoid"));
    }

    private Object sleepFiberObject() throws InterruptedException {
        Fiber.sleep(10);
        return new Object();
    }

    private Object skipForwardsToSuspendableObject() throws InterruptedException {
        return sleepFiberObject();
    }

    @Test
    public final void testSkipForwardsToSuspendableObject() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::skipForwardsToSuspendableObject).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableObject"));
    }

    private double sleepFiberDouble() throws InterruptedException {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextDouble();
    }

    private double skipForwardsToSuspendableDouble() throws InterruptedException {
        return sleepFiberDouble();
    }

    @Test
    public final void testSkipForwardsToSuspendableDouble() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::skipForwardsToSuspendableDouble).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableDouble"));
    }

    private float sleepFiberFloat() throws InterruptedException {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextFloat();
    }

    private float skipForwardsToSuspendableFloat() throws InterruptedException {
        return sleepFiberFloat();
    }

    @Test
    public final void testSkipForwardsToSuspendableFloat() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::skipForwardsToSuspendableFloat).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableFloat"));
    }

    private int sleepFiberInt() throws InterruptedException {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextInt();
    }

    private int skipForwardsToSuspendableInt() throws InterruptedException {
        return sleepFiberInt();
    }

    @Test
    public final void testSkipForwardsToSuspendableInt() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::skipForwardsToSuspendableInt).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableInt"));
    }

    private long sleepFiberLong() throws InterruptedException {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextLong();
    }

    private long skipForwardsToSuspendableLong() throws InterruptedException {
        return sleepFiberLong();
    }

    @Test
    public final void testSkipForwardsToSuspendableLong() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::skipForwardsToSuspendableLong).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableLong"));
    }

    private Object dontSkipForwardsWithTryCatch() throws InterruptedException {
        try {
            return sleepFiberObject();
        } catch (SuspendExecution e) {
            return null;
        }
    }

    @Test
    public final void testDontSkipForwardsWithTryCatch() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::dontSkipForwardsWithTryCatch).start().join();
        assertFalse(isOptimized("skipForwardsWithTryCatch"));
    }

    private void dontSkipForwardsWithLoop() throws InterruptedException {
        for (int i = 0; i < 3; i++)
            sleepFiberVoid();
    }

    @Test
    public final void testDontSkipForwardsWithLoop() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::dontSkipForwardsWithLoop).start().join();
        assertFalse(isOptimized("skipForwardsWithLoop"));
    }

    private void dontSkipForwardsWithLoopBefore() throws InterruptedException {
        for (int i = 0; i < 5; i++)
            i++;
        sleepFiberVoid();
    }

    @Test
    public final void testDontSkipForwardsWithLoopBefore() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::dontSkipForwardsWithLoopBefore).start().join();
        assertFalse(isOptimized("skipForwardsWithLoopBefore"));
    }

    private void skipForwardsWithLoopAfter() throws InterruptedException {
        sleepFiberVoid();
        for (int i = 0; i < 3; i++)
            System.nanoTime();
    }

    @Test
    public final void testSkipForwardsWithLoopAfter() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::skipForwardsWithLoopAfter).start().join();
        assertTrue(isOptimized("skipForwardsWithLoopAfter"));
    }

    private void dontSkipForwardsWithMethodBefore() throws InterruptedException {
        System.nanoTime();
        sleepFiberVoid();
    }

    @Test
    public final void testDontSkipForwardsWithMethodBefore() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::dontSkipForwardsWithMethodBefore).start().join();
        assertFalse(isOptimized("skipForwardsWithMethodBefore"));
    }

    private void skipForwardsWithMethodAfter() throws InterruptedException {
        sleepFiberVoid();
        System.nanoTime();
    }

    @Test
    public final void testSkipForwardsWithMethodAfter() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) this::skipForwardsWithMethodAfter).start().join();
        assertTrue(isOptimized("skipForwardsWithMethodAfter"));
    }

    private void dontSkipForwardsWithReflectiveCalls() throws InterruptedException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        sleepFiberVoid();
        getClass().getMethod("sleepFiberVoid", new Class[0]).invoke(this);
    }

    @Test
    public final void testDontSkipForwardsWithReflectiveCalls() throws InterruptedException, ExecutionException {
        assumeFalse(InstrumentMethod.optimizationDisabled);

        new Fiber((SuspendableRunnable) () -> {
            try {
                dontSkipForwardsWithReflectiveCalls();
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }).start().join();
        assertFalse(isOptimized("skipForwardsWithReflectiveCalls"));
    }
}
