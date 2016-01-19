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

import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author circlespainter
 */
public final class MultipleCallSitesTest extends LiveInstrumentationTest {
    private static class F implements SuspendableCallable<Integer> {
        @Override
        // @Suspendable
        public Integer run() throws InterruptedException {
            final String s = "ciao";
            System.err.println("Enter run(), calling m(" + s + ") and mm(" + s + ")");
            final int ret = m(s);
            assertThat(s, equalTo("ciao"));
            final int ret1 = mm(s);
            System.err.println("Exit run(), called m(" + s + ") and mm(" + s + "), returning " + ret + " + " + ret1 + " = " + (ret + ret1));
            assertThat(s, equalTo("ciao"));
            assertThat(ret, equalTo(2));
            assertThat(ret1, equalTo(2));
            return ret + ret1;
        }

        // @Suspendable
        private static int m(String s) {
            System.err.println("Enter m(" + s + "), calling m1(" + s + ") and mm1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final int ret = m1(s);
            assertThat(s, equalTo("ciao"));
            final int ret1 = mm1(s);
            System.err.println("Exit m(" + s + "), called m1(" + s + ") and mm1(" + s + "), returning " + ret + " + " + ret1 + " = " + (ret + ret1));
            assertThat(s, equalTo("ciao"));
            assertThat(ret, equalTo(1));
            assertThat(ret1, equalTo(1));
            return ret + ret1;
        }

        // @Suspendable
        private static int mm(String s) {
            System.err.println("Enter m(" + s + "), calling m1(" + s + ") and mm1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final int ret = m1(s);
            assertThat(s, equalTo("ciao"));
            final int ret1 = mm1(s);
            System.err.println("Exit m(" + s + "), called m1(" + s + ") and mm1(" + s + "), returning " + ret + " + " + ret1 + " = " + (ret + ret1));
            assertThat(s, equalTo("ciao"));
            assertThat(ret, equalTo(1));
            assertThat(ret1, equalTo(1));
            return ret + ret1;
        }

        // @Suspendable
        private static int m1(String s) {
            System.err.println("Enter m1(" + s + "), sleeping");
            assertThat(s, equalTo("ciao"));
            try {
                Fiber.sleep(10);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.err.println("Exit m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            return 1;
        }

        // @Suspendable
        private static int mm1(String s) {
            System.err.println("Enter mm1(" + s + "), sleeping");
            assertThat(s, equalTo("ciao"));
            try {
                Fiber.sleep(10);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.err.println("Exit mm1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            return 1;
        }
    }

    @Test public void test() {
        final Fiber<Integer> f1 = new Fiber<>(new F()).start();
        try {
            assertThat(f1.get(), equalTo(4));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        final Fiber<Integer> f2 = new Fiber<>(new F()).start();
        try {
            assertThat(f2.get(), equalTo(4));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        assertThat(LiveInstrumentation.getRunCount(), equalTo(3L));
    }
}
