/*
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
package co.paralleluniverse.fibers.instrument;

/**
 *
 * @author pron
 */
final class Debug {
    private static final boolean unitTest;

    static {
        boolean isUnitTest = false;
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stack) {
            if (ste.getClassName().startsWith("org.junit")
                    || ste.getClassName().startsWith("junit.framework")
                    || ste.getClassName().contains("JUnitTestClass")) {
                isUnitTest = true;
                break;
            }
        }
        unitTest = isUnitTest;
    }

    static boolean isUnitTest() {
        return unitTest;
    }

    private Debug() {
    }
}
