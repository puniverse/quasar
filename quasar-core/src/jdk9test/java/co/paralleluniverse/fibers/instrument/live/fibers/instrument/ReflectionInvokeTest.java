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
import co.paralleluniverse.strands.SuspendableCallable;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static co.paralleluniverse.fibers.TestsHelper.exec;
import static org.junit.Assert.assertEquals;

/**
 * Check that a generic catch all does not affect the suspension of a method
 *
 * @author Matthias Mann
 */
public class ReflectionInvokeTest {
    private ArrayList<String> results = new ArrayList<>();

    /** @noinspection unused*/
    private String suspendableMethod() {
        Fiber.park();
        return "hi";
    }

    class Callable1 implements SuspendableCallable<Integer> {
        @Override
        public Integer run() {
            final Method m;
            try {
                m = ReflectionInvokeTest.class.getDeclaredMethod("suspendableMethod");
                m.setAccessible(true);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            try {
                results.add("A");
                m.invoke(ReflectionInvokeTest.this);
                results.add("C");
                m.invoke(ReflectionInvokeTest.this);
                results.add("E");
                return 3;
            } catch (InvocationTargetException | IllegalAccessException ex) {
                //System.out.println("EX: " + ex);
                throw new RuntimeException(ex);
            }
        }
    }

    @Test
    public void testCatch() {
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
    }
}
