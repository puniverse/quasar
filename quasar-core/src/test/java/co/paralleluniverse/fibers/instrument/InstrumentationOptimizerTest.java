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
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;
import static co.paralleluniverse.fibers.instrument.QuasarInstrumentor.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author circlespainter
 */
public class InstrumentationOptimizerTest {

    private boolean isOptimizedAs(String opt, String method) {
        for (Method m: getClass().getDeclaredMethods()) {
            if (method.equals(m.getName())) {
                Instrumented i = m.getAnnotation(Instrumented.class);
                return opt.equals(i.methodOptimization());
            }
        }
        return false;
    }

    public void sleepFiberVoid() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
    }

    public void skipForwardsToSuspendableVoid() throws InterruptedException, SuspendExecution {
        sleepFiberVoid();
    }

    @Test
    public void testSkipForwardsToSuspendableVoid() {
        assertTrue(isOptimizedAs(OPT_FORWARDS_TO_SUSPENDABLE_SKIP, "skipForwardsToSuspendableVoid"));
    }

    public Object sleepFiberObject() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        return new Object();
    }

    public Object skipForwardsToSuspendableObject() throws InterruptedException, SuspendExecution {
        return sleepFiberObject();
    }

    @Test
    public void testSkipForwardsToSuspendableObject() {
        assertTrue(isOptimizedAs(OPT_FORWARDS_TO_SUSPENDABLE_SKIP, "skipForwardsToSuspendableObject"));
    }

    public double sleepFiberDouble() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextDouble();
    }

    public double skipForwardsToSuspendableDouble() throws InterruptedException, SuspendExecution {
        return sleepFiberDouble();
    }

    @Test
    public void testSkipForwardsToSuspendableDouble() {
        assertTrue(isOptimizedAs(OPT_FORWARDS_TO_SUSPENDABLE_SKIP, "skipForwardsToSuspendableDouble"));
    }

    public float sleepFiberFloat() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextFloat();
    }

    public float skipForwardsToSuspendableFloat() throws InterruptedException, SuspendExecution {
        return sleepFiberFloat();
    }

    @Test
    public void testSkipForwardsToSuspendableFloat() {
        assertTrue(isOptimizedAs(OPT_FORWARDS_TO_SUSPENDABLE_SKIP, "skipForwardsToSuspendableFloat"));
    }

    public int sleepFiberInt() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextInt();
    }

    public int skipForwardsToSuspendableInt() throws InterruptedException, SuspendExecution {
        return sleepFiberInt();
    }

    @Test
    public void testSkipForwardsToSuspendableInt() {
        assertTrue(isOptimizedAs(OPT_FORWARDS_TO_SUSPENDABLE_SKIP, "skipForwardsToSuspendableInt"));
    }

    public long sleepFiberLong() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
        return ThreadLocalRandom.current().nextLong();
    }

    public long skipForwardsToSuspendableLong() throws InterruptedException, SuspendExecution {
        return sleepFiberLong();
    }

    @Test
    public void testSkipForwardsToSuspendableLong() {
        assertTrue(isOptimizedAs(OPT_FORWARDS_TO_SUSPENDABLE_SKIP, "skipForwardsToSuspendableLong"));
    }
}
