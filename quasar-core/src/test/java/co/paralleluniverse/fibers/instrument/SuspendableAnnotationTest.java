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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import static co.paralleluniverse.fibers.TestsHelper.exec;

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.junit.Test;

/**
 * Test instrumentation of methods marked with the @Suspendable annotation
 *
 * @author pron
 */
public class SuspendableAnnotationTest {
    private final List<String> results = new ArrayList<>();

    @Suspendable
    private void suspendableMethod() {
        try {
            results.add("A");
            Fiber.park();
            results.add("B");
            Fiber.park();
            results.add("C");
        } catch (SuspendExecution e) {
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
        } catch (SuspendExecution e) {
            throw Exceptions.sneakyThrow(e);
        }
    }

    @Test
    public void testAnnotated() {
        try {
            Fiber co = new Fiber((String) null, null, (SuspendableCallable) null) {
                @Override
                protected Object run() throws SuspendExecution, InterruptedException {
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

        try {
            Fiber co = new Fiber((String) null, null, (SuspendableCallable) null) {
                @Override
                protected Object run() throws SuspendExecution, InterruptedException {
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
