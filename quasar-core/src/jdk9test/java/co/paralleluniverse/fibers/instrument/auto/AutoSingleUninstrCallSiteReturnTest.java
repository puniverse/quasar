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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;

import java.util.concurrent.ExecutionException;

/**
 * @author circlespainter
 */
public class AutoSingleUninstrCallSiteReturnTest {
    static class F implements SuspendableCallable<Integer> {
        @Override
        public Integer run() throws SuspendExecution, InterruptedException {
            System.err.println("Enter run(), calling m()");
            int ret = m();
            System.err.println("Exit run()");
            return ret;
        }

        // @Suspendable
        public int m() {
            System.err.println("Enter m(), calling m1()");
            int ret = m1();
            System.err.println("Exit m()");
            return ret;
        }

        @Suspendable
        public int m1() {
            System.err.println("Enter m1(), sleeping");
            try {
                Fiber.sleep(1000);
            } catch (final InterruptedException | SuspendExecution e) {
                throw new RuntimeException(e);
            }
            System.err.println("Exit m1()");
            return 0;
        }
    }

    // TODO: fixme
    @Ignore @Test public void uniqueMissingCallSite() {
        final Fiber<Integer> f1 = new Fiber<>(new F()).start();
        try {
            assertThat(f1.get(), equalTo(0));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        final Fiber<Integer> f2 = new Fiber<>(new F()).start();
        try {
            assertThat(f2.get(), equalTo(0));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
