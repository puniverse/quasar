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
public class SingleUninstrCallSiteInterfaceTest {
    interface M {
        // @Suspendable
        double m(String s);
    }

    interface M1 {
        // @Suspendable
        double m1(String s);
    }

    static class MImpl implements M {
        private static final M1 m1 = new M1Impl();

        // @Suspendable
        public double m(String s) {
            System.err.println("Enter m(" + s + "), calling M1.m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final M1 m1 = MImpl.m1;
            final double ret = m1.m1(s);
            System.err.println("Exit m(" + s + "), called M1.m1(" + s + ")");
            assertThat(m1, equalTo(MImpl.m1));
            assertThat(s, equalTo("ciao"));
            return ret;
        }
    }

    static class M1Impl implements M1 {
        // @Suspendable
        public double m1(String s) {
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

    static class F implements SuspendableCallable<Double> {
        private static final M m = new MImpl();

        @Override
        // @Suspendable
        public Double run() throws InterruptedException {
            final String s = "ciao";
            System.err.println("Enter run(), calling M.m(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final M m = F.m;
            final double ret = m.m(s);
            System.err.println("Exit run(), called M.m(" + s + ")");
            assertThat(m, equalTo(F.m));
            assertThat(s, equalTo("ciao"));
            return ret;
        }
    }

    @Test public void test() {
        final Fiber<Double> f1 = new Fiber<>(new F()).start();
        try {
            assertThat(f1.get(), equalTo(-1.7));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        final Fiber<Double> f2 = new Fiber<>(new F()).start();
        try {
            assertThat(f2.get(), equalTo(-1.7));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
