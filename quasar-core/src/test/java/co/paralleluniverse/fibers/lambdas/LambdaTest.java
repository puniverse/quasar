/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.lambdas;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

/**
 * @author circlespainter
 */
public class LambdaTest {
    @Suspendable
    @FunctionalInterface
    private interface I {
        @Suspendable
        void doIt();
    }

    private void run(I i) throws ExecutionException, InterruptedException {
        new Fiber() {
            @Override
            protected Object run() throws SuspendExecution, InterruptedException {
                i.doIt();
                return null;
            }
        }.start().join();
    }

    @Test
    public void suspLambda() throws Exception {
        run(() -> {
            try {
                Strand.sleep(10);
            } catch (final SuspendExecution e) {
                throw new AssertionError(e);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
