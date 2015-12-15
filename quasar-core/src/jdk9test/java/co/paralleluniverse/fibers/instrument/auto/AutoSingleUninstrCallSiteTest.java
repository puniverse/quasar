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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;

import java.util.concurrent.ExecutionException;

/**
 * @author circlespainter
 */
/**
 * @author circlespainter
 */
public class AutoSingleUninstrCallSiteTest {
    static class F implements SuspendableRunnable {
        @Override
        public void run() throws SuspendExecution, InterruptedException {
            System.err.println("Enter run(), calling m()");
            m();
            System.err.println("Exit run()");
        }

        // @Suspendable
        public void m() {
            System.err.println("Enter m(), calling m1()");
            m1();
            System.err.println("Exit m()");
        }

        @Suspendable
        public void m1() {
            System.err.println("Enter m1(), sleeping");
            try {
                Fiber.sleep(10);
            } catch (final InterruptedException | SuspendExecution e) {
                throw new RuntimeException(e);
            }
            System.err.println("Exit m1()");
        }
    }

    @Test public void uniqueMissingCallSite() {
        final Fiber f1 = new Fiber(new F()).start();
        try {
            f1.join();
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        final Fiber f2 = new Fiber(new F()).start();
        try {
            f2.join();
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
