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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.instrument.live.LiveInstrumentationTest;
import co.paralleluniverse.strands.SuspendableCallable;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static co.paralleluniverse.fibers.TestsHelper.exec;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Check that a generic catch all does not affect the suspension of a method
 *
 * @author Matthias Mann
 */
public final class ReflectionInvokeTest extends LiveInstrumentationTest {
    private ArrayList<String> results = new ArrayList<>();

    /** @noinspection unused*/
    private String suspendableMethod() {
        System.out.println("Parking");
        Fiber.park();
        return "hi";
    }

    private final class Callable1 implements SuspendableCallable<Integer> {
        @Override
        public final Integer run() {
            final Method m;
            try {
                m = ReflectionInvokeTest.class.getDeclaredMethod("suspendableMethod");
                m.setAccessible(true);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            try {
                results.add("A");
                System.out.println("Invoking 1");
                m.invoke(ReflectionInvokeTest.this);
                results.add("C");
                System.out.println("Invoking 2");
                m.invoke(ReflectionInvokeTest.this);
                results.add("E");
                return 3;
            } catch (final InvocationTargetException | IllegalAccessException ex) {
                //System.out.println("EX: " + ex);
                throw new RuntimeException(ex);
            }
        }
    }

    @Test
    public final void testCatch() {
        results.clear();

        try {
            final Fiber co = new Fiber<>((String) null, null, new Callable1());
            exec(co);
            results.add("B");
            exec(co);
            results.add("D");
            exec(co);
        } finally {
            System.out.println(results);
        }

        assertEquals(5, results.size());
        assertEquals(Arrays.asList(
                "A",
                "B",
                "C",
                "D",
                "E"), results);

        assertThat(LiveInstrumentation.fetchRunCount(), equalTo(1L));
    }
}
