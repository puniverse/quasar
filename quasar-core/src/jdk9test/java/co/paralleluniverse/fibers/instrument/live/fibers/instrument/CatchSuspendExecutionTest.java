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
package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static co.paralleluniverse.fibers.TestsHelper.exec;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 * Test instrumentation of unmarked methods catching `SuspendExecution`
 *
 * @author pron
 */
public class CatchSuspendExecutionTest {
    private final List<String> results = new ArrayList<>();

    public void suspendableMethod() {
        try {
            results.add("A");
            Fiber.park();
            results.add("B");
            Fiber.park();
            results.add("C");
        } catch (final SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    private void nonsuspendableMethod() {
        try {
            results.add("A");
            Fiber.park();
            results.add("B");
            Fiber.park();
            results.add("C");
        } catch (final SuspendExecution e) {
            throw Exceptions.sneakyThrow(e);
        }
    }

    @Test
    public void testAnnotated() {
        try {
            final Fiber<Object> co = new Fiber<>((String) null, null, (SuspendableCallable<Object>) null) {
                @Override
                protected Object run() throws InterruptedException {
                    suspendableMethod();
                    return null;
                }
            };
            exec(co);
            exec(co);
            exec(co);
        } finally {
            System.out.println(results);
        }

        assertEquals(3, results.size());
        assertEquals(Arrays.asList("A", "B", "C"), results);
    }

    @Test
    public void testNonAnnotated() {
        assumeFalse(SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation"));
        assumeFalse(SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.instrument.live.enable"));

        try {
            final Fiber<Object> co = new Fiber<>((String) null, null, (SuspendableCallable<Object>) null) {
                @Override
                protected Object run() throws InterruptedException {
                    nonsuspendableMethod();
                    return null;
                }
            };
            exec(co);
            exec(co);
            exec(co);
        } finally {
            System.out.println(results);
        }

        assertEquals(3, results.size());
        assertEquals(Arrays.asList("A", "A", "A"), results);
    }
}
