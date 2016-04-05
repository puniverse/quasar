/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.VerifyInstrumentationException;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.junit.Test;

/**
 *
 * @author circlespainter
 */
public final class VerificationTest {
    interface I1 {
        void doIt();
    }

    @Suspendable
    interface I2 {
        void doIt();
    }

    private static abstract class A {
        protected abstract void doItAbstr();
    }

    private static final class C extends A implements I1, I2 {
        @Override
        @Suspendable
        public final void doIt() {
            try {
                Fiber.sleep(10);
            } catch (final InterruptedException | SuspendExecution e) {
                throw new AssertionError(e);
            }
        }

        @Override
        @Suspendable
        protected final void doItAbstr() {
            try {
                Fiber.sleep(10);
            } catch (final InterruptedException | SuspendExecution e) {
                throw new AssertionError(e);
            }
        }
    }

    private void doUninstrumented() throws Exception {
        Fiber.sleep(10);
    }

    private void doInstrumented() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
    }

    @Test
    public final void testVerification() throws ExecutionException, InterruptedException {
        assumeTrue(SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation"));

        final I1 i1 = new C();
        final I2 i2 = (C) i1;
        final A a = (C) i1;
        
        Throwable t = null;

        final Fiber<?> fUninstrumentedMethod1 = new Fiber<>(new SuspendableRunnable() { @Override public final void run() throws SuspendExecution, InterruptedException {
            try {
                doUninstrumented(); // **
                Fiber.sleep(10);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }}).start();
        try {
            fUninstrumentedMethod1.join();
        } catch (final ExecutionException re) {
            t = re.getCause().getCause();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" **"));

        final Fiber<?> fUninstrumentedMethod2 = new Fiber(new SuspendableRunnable() { @Override public final void run() throws SuspendExecution, InterruptedException {
            try {
                i1.doIt();
            } finally {
                i1.doIt();
            }
        }}).start();
        try {
            fUninstrumentedMethod2.join();
        } catch (final ExecutionException re) {
            t = re.getCause();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" **"));

        final Fiber<?> fUninstrumentedCallSite1 = new Fiber<>(new SuspendableRunnable() { @Override public final void run() throws SuspendExecution, InterruptedException {
            try {
                Fiber.sleep(10);
                i1.doIt(); // !!
            } finally {
                i1.doIt();
            }
        }}).start();
        try {
            fUninstrumentedCallSite1.join();
        } catch (final ExecutionException re) {
            t = re.getCause();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" !! ("));

        final Fiber<?> fUninstrumentedCallSite2 = new Fiber<>(new SuspendableRunnable() { @Override public final void run() throws SuspendExecution, InterruptedException {
            try {
                Fiber.sleep(10);
                a.doItAbstr(); // !!
            } finally {
                a.doItAbstr();
            }
        }}).start();
        try {
            fUninstrumentedCallSite2.join();
        } catch (final ExecutionException re) {
            t = re.getCause();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" !! ("));

        final Fiber<Integer> fOk = new Fiber<>(new SuspendableCallable<Integer>() { @Override public final Integer run() throws SuspendExecution, InterruptedException {
            Fiber.sleep(10);
            i2.doIt();
            doInstrumented();
            return 4;
        }}).start();
        assertEquals(fOk.get(), new Integer(4));
    }

    @Suspendable
    private void doInstrumentedExc() {
        try {
            Fiber.sleep(10);
            throw new NullPointerException("something is broken");
        } catch (final InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (final SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    private void doUninstrumentedExc() {
        doInstrumentedExc();
    }

    @Test
    public final void testVerificationExc() throws ExecutionException, InterruptedException {
        assumeTrue(!SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation"));

        final Fiber<?> f = new Fiber<>(new SuspendableRunnable() { @Override public final void run() throws SuspendExecution, InterruptedException {
            doUninstrumentedExc(); // **
            Fiber.sleep(10);
        }}).start();
        try {
            f.join();
        } catch (final ExecutionException re) {
            assertTrue(re.getCause().getSuppressed()[0].getMessage().contains(" **"));
        }
    }
}
