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
package co.paralleluniverse.fibers.instrument.auto;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author circlespainter
 */
public class AutoMultipleSameUninstrCallSiteTest {
    static class F implements SuspendableCallable<Double> {
        @Override
        public Double run() throws SuspendExecution, InterruptedException {
            final String s = "ciao";
            System.err.println("Enter run(), calling m(" + s + ") twice");
            final double ret = m(s); double ret1 = m(s);
            System.err.println("Exit run(), called m(" + s + ")");
            return ret + ret1;
        }

        // @Suspendable
        public static double m(String s) {
            System.err.println("Enter m(" + s + "), calling m1(" + s + ")");
            final double ret = m1(s); double ret1 = m1(s);
            System.err.println("Exit m(" + s + "), called m1(" + s + ")");
            return ret + ret1;
        }

        @Suspendable
        public static double m1(String s) {
            System.err.println("Enter m1(" + s + "), sleeping");
            try {
                Fiber.sleep(10);
            } catch (final InterruptedException | SuspendExecution e) {
                throw new RuntimeException(e);
            }
            System.err.println("Exit m1(" + s + ")");
            return -1.7;
        }
    }

    @Test public void uniqueMissingCallSiteReturn() {
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
