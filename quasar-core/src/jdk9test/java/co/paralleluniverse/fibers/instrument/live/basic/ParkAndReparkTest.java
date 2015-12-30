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
import co.paralleluniverse.strands.SuspendableCallable;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static co.paralleluniverse.fibers.TestsHelper.exec;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author circlespainter
 */
public class ParkAndReparkTest {
    static class F implements SuspendableCallable<Double> {
        @Override
        // @Suspendable
        public Double run() throws InterruptedException {
            final String s = "ciao";
            System.err.println("Enter run(), calling m(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final double ret = m(s);
            System.err.println("Exit run(), called m(" + s + ")");
            assertThat(s, equalTo("ciao"));
            return ret;
        }

        // @Suspendable
        public double m(String s) {
            System.err.println("Enter m(" + s + "), calling m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final double ret = m1(s);
            System.err.println("Exit m(" + s + "), called m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            return ret;
        }

        // @Suspendable
        public double m1(String s) {
            System.err.println("Enter m1(" + s + "), parking several times");
            Fiber.park();
            assertThat(s, equalTo("ciao"));
            Fiber.park();
            System.err.println("Exit m1(" + s + "), parking several times");
            Fiber.park();
            assertThat(s, equalTo("ciao"));
            Fiber.park();
            return -1.7;
        }
    }

    @Test public void test() {
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
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

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
    }
}
