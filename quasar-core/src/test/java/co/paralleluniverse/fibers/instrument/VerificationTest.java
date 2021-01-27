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
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.VerifyInstrumentationException;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.IntChannel;
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

    private static abstract class A1 {
        protected abstract void doItAbstr();
    }

    private static abstract class A2 {
        protected abstract void doItAbstr1();
        @Suspendable
        protected abstract void doItAbstr2();
    }


    private static final class C1 extends A1 implements I1, I2 {
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

    private static final class C2 extends A2 implements I1, I2 {
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
        protected final void doItAbstr1() {
            try {
                Fiber.sleep(10);
            } catch (final InterruptedException | SuspendExecution e) {
                throw new AssertionError(e);
            }
        }

        @Override
        @Suspendable
        protected final void doItAbstr2() {
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
    public final void testVerifyUninstrumentedMethod() throws ExecutionException, InterruptedException {
        assumeTrue(SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation"));

        final I1 i1 = new C1();

        Throwable t = null;

        final Fiber<?> fUninstrumentedMethod1 = new Fiber<>(new SuspendableRunnable() {
            @Override
            public final void run() throws SuspendExecution, InterruptedException {
                try {
                    doUninstrumented(); // **
                    Fiber.sleep(10);
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        try {
            fUninstrumentedMethod1.join();
        } catch (final ExecutionException re) {
            t = re.getCause().getCause();
            t.printStackTrace();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" **"));

        final Fiber<?> fUninstrumentedMethod2 = new Fiber(new SuspendableRunnable() {
            @Override
            public final void run() throws SuspendExecution, InterruptedException {
                try {
                    i1.doIt();
                } finally {
                    i1.doIt();
                }
            }
        }).start();
        try {
            fUninstrumentedMethod2.join();
        } catch (final ExecutionException re) {
            t = re.getCause();
            t.printStackTrace();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" **"));
    }

    @Test
    public final void testVerifyUninstrumentedCallSite() throws ExecutionException, InterruptedException {
        assumeTrue(SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation"));

        final I1 i1 = new C1();
        final A1 a1 = (C1) i1;

        Throwable t = null;

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
            t.printStackTrace();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" !! ("));

        final Fiber<?> fUninstrumentedCallSite2 = new Fiber<>(new SuspendableRunnable() { @Override public final void run() throws SuspendExecution, InterruptedException {
            try {
                Fiber.sleep(10);
                a1.doItAbstr(); // !!
            } finally {
                a1.doItAbstr();
            }
        }}).start();
        try {
            fUninstrumentedCallSite2.join();
        } catch (final ExecutionException re) {
            t = re.getCause();
            t.printStackTrace();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" !! ("));
    }

    @Test
    public final void testVerifyUninstrumentedCallSiteSameSourceLine() throws ExecutionException, InterruptedException {
        assumeTrue(SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation"));

        final A2 a2 = new C2();

        Throwable t = null;

        final Fiber<?> fUninstrumentedCallSite3 = new Fiber<>(new SuspendableRunnable() { @Override public final void run() throws SuspendExecution, InterruptedException {
            try {
                Fiber.sleep(10);
                a2.doItAbstr1(); a2.doItAbstr2(); // !!
            } finally {
                a2.doItAbstr1(); a2.doItAbstr2();
            }
        }}).start();
        try {
            fUninstrumentedCallSite3.join();
        } catch (final ExecutionException re) {
            t = re.getCause();
            t.printStackTrace();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" !! ("));
    }

    @Test
    public final void testVerificationOK() throws ExecutionException, InterruptedException {
        final I1 i1 = new C1();
        final I2 i2 = (C1) i1;

        final Fiber<Integer> fOk = new Fiber<>(new SuspendableCallable<Integer>() { @Override public final Integer run() throws SuspendExecution, InterruptedException {
            Fiber.sleep(10);
            i2.doIt();
            doInstrumented();
            return 4;
        }}).start();
        assertEquals(fOk.get(), new Integer(4));
    }

    @Test
    public final void testVerifyUninstrumentedCallSiteDeclaringAndOwnerOK() throws ExecutionException, InterruptedException, SuspendExecution {
        assumeTrue(SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation"));

        // From https://github.com/puniverse/quasar/issues/255
        final IntChannel intChannel = Channels.newIntChannel(1);
        try {
            new Fiber<>(new SuspendableCallable<Integer>() {
                @Override
                public Integer run() throws SuspendExecution, InterruptedException {
                    return intChannel.receive();
                }
            }).start().join(100, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException ignored) {
        }
        // Should complete without verification exceptions
    }
}
