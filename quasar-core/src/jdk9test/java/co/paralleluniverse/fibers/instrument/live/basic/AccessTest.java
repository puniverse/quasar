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
package co.paralleluniverse.fibers.instrument.live.basic;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.instrument.live.LiveInstrumentationTest;
import co.paralleluniverse.strands.SuspendableCallable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static co.paralleluniverse.fibers.TestsHelper.exec;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

/**
 * @author circlespainter
 */
public final class AccessTest extends LiveInstrumentationTest {
    private static List<Integer> l = new ArrayList<>();

    private static final class F implements SuspendableCallable<Double> {
        @Override
        // @Suspendable
        public final Double run() throws InterruptedException {
            System.err.println("Invoking accessor");
            l.size();
            final String s = "ciao";
            System.err.println("Enter run(), calling m(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final double ret = m(s);
            System.err.println("Exit run(), called m(" + s + ")");
            assertThat(s, equalTo("ciao"));
            return ret;
        }

        // @Suspendable
        private double m(String s) {
            System.err.println("Invoking accessor");
            l.size();
            System.err.println("Enter m(" + s + "), calling m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final double ret = m1(s);
            System.err.println("Exit m(" + s + "), called m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            return ret;
        }

        // @Suspendable
        private double m1(String s) {
            System.err.println("Invoking accessor");
            l.size();
            System.err.println("Enter m1(" + s + "), parking several times");
            Fiber.park();
            l.add(1);
            assertThat(s, equalTo("ciao"));
            Fiber.park();
            l.add(2);
            System.err.println("Exit m1(" + s + "), parking several times");
            Fiber.park();
            l.add(3);
            assertThat(s, equalTo("ciao"));
            Fiber.park();
            l.add(4);
            return -1.7;
        }
    }

    @Test public final void test() {
        final Fiber<Double> f1 = new Fiber<>((String) null, null, new F());
        System.err.println("Run f1");
        exec(f1);
        System.err.println("Run f1");
        exec(f1);
        System.err.println("Run f1");
        exec(f1);
        System.err.println("Run f1");
        exec(f1);
        System.err.println("Run f1");
        exec(f1);
        System.err.println("Get f1");
        try {
            assertThat(f1.get(), equalTo(-1.7));
            assertThat(l.size(), equalTo(4));
            assertEquals(Arrays.asList(1, 2, 3, 4), l);
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        l.clear();

        final Fiber<Double> f2 = new Fiber<>((String) null, null, new F());
        System.err.println("Run f2");
        exec(f2);
        System.err.println("Run f2");
        exec(f2);
        System.err.println("Run f2");
        exec(f2);
        System.err.println("Run f2");
        exec(f2);
        System.err.println("Run f2");
        exec(f2);
        System.err.println("Get f2");
        try {
            assertThat(f2.get(), equalTo(-1.7));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        assertThat(l.size(), equalTo(4));
        assertEquals(Arrays.asList(1, 2, 3, 4), l);

        assertThat(LiveInstrumentation.fetchRunCount(), equalTo(1L));
    }
}
