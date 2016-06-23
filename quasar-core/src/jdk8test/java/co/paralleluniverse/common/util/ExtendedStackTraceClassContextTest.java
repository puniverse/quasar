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
package co.paralleluniverse.common.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author candrews
 */
public class ExtendedStackTraceClassContextTest {
    // Commented out temporarily to test on pre-8u60
/*
    @Test
    public void lambdaInCallStackTest() throws Exception {
        ((Runnable) (() -> {
            new ExtendedStackTraceClassContext().get();
        })).run();
    }
    @Test
    public void NoLambdaInCallStackTest() throws Exception {
        new ExtendedStackTraceClassContext().get();
    }
*/
}

