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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author circlespainter
 */
public class MultipleIdenticalCallSitesTest {
    static class F implements SuspendableCallable<Double> {
        @Override
        // @Suspendable
        public Double run() throws InterruptedException {
            final String s = "ciao";
            System.err.println("Enter run(), calling m(" + s + ") twice");
            final double ret = m(s);
            assertThat(s, equalTo("ciao"));
            final double ret1 = m(s);
            assertThat(ret, equalTo(-3.4));
            assertThat(s, equalTo("ciao"));
            System.err.println("Exit run(), called m(" + s + ")");
            return ret + ret1;
        }

        // @Suspendable
        public static double m(String s) {
            System.err.println("Enter m(" + s + "), calling m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final double ret = m1(s);
            assertThat(s, equalTo("ciao"));
            final double ret1 = m1(s);
            System.err.println("Exit m(" + s + "), called m1(" + s + ")");
            assertThat(ret, equalTo(-1.7));
            assertThat(s, equalTo("ciao"));
            return ret + ret1;
        }

        // @Suspendable

        public static double m1(String s) {
            System.err.println("Enter m1(" + s + "), sleeping");
            assertThat(s, equalTo("ciao"));
            try {
                Fiber.sleep(10);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.err.println("Exit m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            return -1.7;
        }
    }

    @Test public void test() {
        final Fiber<Double> f1 = new Fiber<>(new F()).start();
        try {
            assertThat(f1.get(), equalTo(-6.8));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        final Fiber<Double> f2 = new Fiber<>(new F()).start();
        try {
            assertThat(f2.get(), equalTo(-6.8));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
