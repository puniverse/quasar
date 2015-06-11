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
import co.paralleluniverse.strands.SuspendableRunnable;
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
        for (Method m: getClass().getDeclaredMethods()) {
            if (method.equals(m.getName())) {
                Instrumented i = m.getAnnotation(Instrumented.class);
                return i != null && i.methodOptimized();
            }
        }
        return false;
    }

    private void sleepFiberVoid() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
    }

    private void skipForwardsToSuspendableVoid() throws InterruptedException, SuspendExecution {
        sleepFiberVoid();
    }

    @Test
    public void testSkipForwardsToSuspendableVoid() throws InterruptedException, SuspendExecution, ExecutionException {
        new Fiber(new SuspendableRunnable() {
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
        new Fiber(new SuspendableRunnable() {
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
        new Fiber(new SuspendableRunnable() {
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
        new Fiber(new SuspendableRunnable() {
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
        new Fiber(new SuspendableRunnable() {
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
        new Fiber(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                skipForwardsToSuspendableLong();
            }
        }).start().join();
        assertTrue(isOptimized("skipForwardsToSuspendableLong"));
    }
}
