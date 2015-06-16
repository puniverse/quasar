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
import co.paralleluniverse.fibers.VerifyInstrumentationException;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author circlespainter
 */
public class UninstrumentedTest {
    public void doUninstrumented() throws Exception {
        Fiber.sleep(10);
    }

    @Test @Ignore
    public void testVerification() throws ExecutionException, InterruptedException {
        assumeTrue(!SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.verifyInstrumentation"));

        final VerificationTest.I1 i1 = new VerificationTest.C();
        final VerificationTest.I2 i2 = (VerificationTest.C) i1;
        
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
            t = re.getCause();
        }
        assertTrue(t instanceof VerifyInstrumentationException && t.getMessage().contains(" !! ("));
    }
}
