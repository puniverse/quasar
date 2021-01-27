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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.Type;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class SuspendablesScannerTest {
    private static SuspendablesScanner scanner;
    private static final Set<String> suspendables = new HashSet<>();
    private static final Set<String> suspendableSupers = new HashSet<>();

    @BeforeClass
    public static void buildGraph() throws Exception {
        // find test classes directory
        final Path project;
        {
            final String resource = SuspendablesScannerTest.class.getName().replace('.', '/') + ".class";
            final URL url = SuspendablesScannerTest.class.getClassLoader().getResource(resource);
            final Path p1 = Paths.get(resource);
            final Path p2 = Paths.get(url.toURI()).toAbsolutePath();
            final Path p = p2.getRoot().resolve(p2.subpath(0, p2.getNameCount() - p1.getNameCount()));
            project = p;
        }
        System.out.println("Test classes: " + project);
        
        // find quasar core directory
        final Path core;
        {
            final String resource = Fiber.class.getName().replace('.', '/') + ".class";
            final URL url = Fiber.class.getClassLoader().getResource(resource);
            final Path p1 = Paths.get(resource);
            final Path p2 = Paths.get(url.toURI()).toAbsolutePath();
            final Path p = p2.getRoot().resolve(p2.subpath(0, p2.getNameCount() - p1.getNameCount()));
            core = p;
        }
        System.out.println("Core classes: " + core);

        scanner = new SuspendablesScanner(project);
//        scanner = new AutoSuspendablesScanner(
//                Paths.get(AutoSuspendablesScannerTest.class.getClassLoader()
//                        .getResource(AutoSuspendablesScannerTest.class.getName().replace('.', '/') + ".class").toURI()));
        
        scanner.setURLs(Collections.singletonList(core.toUri().toURL()));
        scanner.setAuto(true);
        scanner.run();
        scanner.putSuspendablesAndSupers(suspendables, suspendableSupers);

        System.out.println("SUSPENDABLES: " + suspendables);
        System.out.println("SUPERS: " + suspendableSupers);
    }

    @Test
    public void suspendableCallTest() {
        final String method = B.class.getName() + ".foo(I)V";
        assertTrue(suspendables.contains(method));
    }

    @Test
    public void superSuspendableCallTest() {
        final String method = A.class.getName() + ".foo" + Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(IA.class));
        assertTrue(suspendables.contains(method));
    }

    @Test
    public void nonSuperSuspendableCallTest() {
        final String method = A.class.getName() + ".foo()";
        assertTrue(!suspendables.contains(method));
    }

    @Test
    public void superNonSuspendableCallTest() {
        final String method = A.class.getName() + ".bar" + Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(IA.class));
        assertTrue(!suspendables.contains(method));
    }

    @Test
    public void inheritedSuspendableCallTest() {
        final String method = C.class.getName() + ".bax" + Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(IA.class));
        assertTrue(suspendables.contains(method));
    }

    @Test
    public void inheritedNonSuspendableCallTest() {
        final String method = C.class.getName() + ".fon()";
        assertTrue(!suspendables.contains(method));
    }

    @Test
    public void superSuspendableTest() {
        final String method = IA.class.getName() + ".foo(I)V";
        assertTrue(suspendableSupers.contains(method));
    }

    @Test
    public void bridgeMethodTest() {
        assertTrue(suspendableSupers.contains(I2.class.getName() + ".foo(I)Ljava/lang/Number;"));
        assertTrue(suspendableSupers.contains(A2.class.getName() + ".bar(I)Ljava/lang/Object;"));
        assertTrue(!suspendableSupers.contains(A2.class.getName() + ".baz(I)Ljava/lang/Object;"));
    }

    static interface IA {
        // super suspendable
        void foo(int t);

        // doesn't have suspandable implementation
        void bar(int t);
    }

    static class A {
        // suspendable
        void foo(IA a) {
            a.foo(0);
        }

        // not suspendable
        void foo() {
            bar(null); // test that if foo->bar->foo->... doesn't cause infinite loop
        }

        // not suspendable
        void bar(IA a) {
            a.bar(0);
            foo();
        }

        // suspendable
        void baz(IA a) {
            a.foo(0);
        }
    }

    static class B implements IA {
        // suspendable
        @Override
        public void foo(int t) {
            try {
                Fiber.park();
            } catch (SuspendExecution ex) {
                throw new RuntimeException(ex);
            }
        }

        // not suspendable
        @Override
        public void bar(int t) {
        }
    }

    static class C extends A {
        // suspendable
        void bax(IA a) {
            baz(a);
        }

        // non suspendable
        void fon() {
            foo();
        }
    }

    static interface I2 {
        Number foo(int x);
    }

    static class A2 {
        protected Object bar(int x) {
            return null;
        }

        protected Object baz(int x) {
            return null;
        }
    }

    static class C2 extends A2 implements I2 {
        @Override
        @Suspendable
        protected String bar(int x) {
            return "3";
        }

        @Override
        @Suspendable
        public Integer foo(int x) {
            return 3;
        }

        public Integer baz(int x) {
            return 3;
        }
    }
}
