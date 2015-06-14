/*
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
package co.paralleluniverse.common.util;

// import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;

/**
 *
 * @author pron
 */
public class ExtendedStackTraceTest {

    public ExtendedStackTraceTest() {
    }

    private static boolean isHotSpotSupported() {
        try {
            new ExtendedStackTraceHotSpot(new Throwable());
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Test
    public void testHotSpot1() {
        Assume.assumeTrue(isHotSpotSupported());
        try {
            new A().foo();
        } catch (Exception e) {
            ExtendedStackTrace st = new ExtendedStackTraceHotSpot(e);

            for (ExtendedStackTraceElement este : st) {
                Member m = este.getMethod();
                assertNotNull(este.getClassName() + "." + este.getMethodName(), m);
            }
        }
    }

    @Test
    public void testPlain() {
        try {
            new A().foo();
        } catch (Exception e) {
            ExtendedStackTrace st = new ExtendedStackTrace(e);

            for (ExtendedStackTraceElement este : st) {
                Member m = este.getMethod();
                if (!skipJunit(este))
                    assertNotNull(este.getClassName() + "." + este.getMethodName(), m);
            }
        }
    }

    @Test
    public void testSecurity() {
        ExtendedStackTrace st = new ExtendedStackTraceClassContext();

        for (ExtendedStackTraceElement este : st) {
            Member m = este.getMethod();
            if (!ExtendedStackTraceClassContext.skipSTE(este.getStackTraceElement()))
                assertNotNull(este.getClassName() + "." + este.getMethodName(), m);
        }
    }

    @Test
    public void testAll() {
        ExtendedStackTraceElement[] plain = new ExtendedStackTrace(new Throwable()).get();
        ExtendedStackTraceElement[] hotspot = isHotSpotSupported() ? new ExtendedStackTraceHotSpot(new Throwable()).get() : null;
        ExtendedStackTraceElement[] security = new ExtendedStackTraceClassContext().get();

        int length = plain.length;
        if (hotspot != null)
            assertEquals(length, hotspot.length);
        assertEquals(length, security.length);

        for (int i = 0; i < plain.length; i++) {
            if (hotspot != null)
                assertNotNull(hotspot[i].getMethodName(), hotspot[i].getMethodName());
            if (!ExtendedStackTraceClassContext.skipSTE(security[i].getStackTraceElement()))
                assertNotNull(security[i].getMethodName(), security[i].getMethodName());
            if (!skipJunit(plain[i]))
                assertNotNull(plain[i].getMethodName(), plain[i].getMethodName());

            Member m = null;
            if (hotspot != null)
                m = hotspot[i].getMethod();
            else if (!ExtendedStackTraceClassContext.skipSTE(security[i].getStackTraceElement()))
                m = security[i].getMethod();
            else if (!skipJunit(plain[i]))
                m = plain[i].getMethod();

            if (m != null) {
                if (!skipJunit(plain[i]))
                    assertEquals("" + i, m, plain[i].getMethod());
                if (!ExtendedStackTraceClassContext.skipSTE(security[i].getStackTraceElement()))
                    assertEquals("" + i, m, security[i].getMethod());
            }
        }
    }

    private boolean skipJunit(ExtendedStackTraceElement este) {
        final String name = este.getClassName();
        return name.contains("junit.") || name.contains("gradle.") || name.contains("com.sun.proxy");
    }

    private static class A {
        private final B b = new B();

        public void foo() {
            b.car();
        }

        public void foo(int x) {
            b.baz();
        }

        public void bar() {
            b.baz(3);
        }
    }

    private static class B {
        public void baz() {
            throw new RuntimeException();
        }

        public void baz(int x) {
            throw new RuntimeException();
        }

        public void car() {
            throw new RuntimeException();
        }
    }

}
