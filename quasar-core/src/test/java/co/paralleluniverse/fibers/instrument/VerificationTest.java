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
public class VerificationTest {
    interface I1 {
        void doIt();
    }

    @Suspendable
    interface I2 {
        void doIt();
    }

    static class C implements I1, I2 {
        @Override
        @Suspendable
        public void doIt() {
            try {
                Fiber.sleep(10);
            } catch (InterruptedException | SuspendExecution e) {
                throw new AssertionError(e);
            }
        }
    }

    public void doUninstrumented() throws Exception {
        Fiber.sleep(10);
    }

    public void doInstrumented() throws InterruptedException, SuspendExecution {
        Fiber.sleep(10);
    }

    @Test
    public void testVerification() throws ExecutionException, InterruptedException {
        assumeFalse(SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation"));

        final I1 i1 = new C();
        final I2 i2 = (C) i1;
        
        Throwable t = null;

        Fiber fUninstrumentedMethod1 = new Fiber(new SuspendableRunnable() { @Override public void run() throws SuspendExecution, InterruptedException {
            try {
                doUninstrumented(); // **
                Fiber.sleep(10);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }}).start();
        try {
            fUninstrumentedMethod1.join();
        } catch (ExecutionException re) {
            t = re.getCause().getCause();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" **"));

        Fiber fUninstrumentedMethod2 = new Fiber(new SuspendableRunnable() { @Override public void run() throws SuspendExecution, InterruptedException {
            try {
                i1.doIt();
            } finally {
                i1.doIt();
            }
        }}).start();
        try {
            fUninstrumentedMethod2.join();
        } catch (ExecutionException re) {
            t = re.getCause();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" **"));

        Fiber<Integer> fUninstrumentedCallSite = new Fiber(new SuspendableRunnable() { @Override public void run() throws SuspendExecution, InterruptedException {
            try {
                Fiber.sleep(10);
                i1.doIt(); // !!
            } finally {
                i1.doIt();
            }
        }}).start();
        try {
            fUninstrumentedCallSite.join();
        } catch (ExecutionException re) {
            t = re.getCause();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" !! ("));

        Fiber<Integer> fOk = new Fiber<>(new SuspendableCallable<Integer>() { @Override public Integer run() throws SuspendExecution, InterruptedException {
            Fiber.sleep(10);
            i2.doIt();
            doInstrumented();
            return 4;
        }}).start();
        assertEquals(fOk.get(), new Integer(4));
    }
}
