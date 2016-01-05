/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

/**
 * A test for issue #73: https://github.com/puniverse/quasar/issues/73
 *
 * @author circlespainter
 */
public class RTInitLocalArrayArgTest implements SuspendableCallable<Object> {
    @Suspendable // Instrumentation is needed to break
    private static Object myMethod(Object arg) {
        return arg;
    }

    public Object run() throws InterruptedException {
        Object arg = null;

        // Any runtime check needed here to break instrumentation
        if (System.getProperties() != null) {
            arg = new Object[0];
            // This doesn't break
            // arg = new Object();
        }

        // Passing a copy of the args bound to a local is needed to break instrumentation
        return myMethod(arg);
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        assertTrue(new Fiber<>(this).start().get() != null);
    }
}
