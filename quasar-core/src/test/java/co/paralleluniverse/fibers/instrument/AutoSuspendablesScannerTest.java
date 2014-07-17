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
import co.paralleluniverse.fibers.SuspendExecution;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

public class AutoSuspendablesScannerTest {
    private static AutoSuspendablesScanner scanner;

    @BeforeClass
    public static void buildGraph() {
        scanner = new AutoSuspendablesScanner((URLClassLoader) AutoSuspendablesScannerTest.class.getClassLoader());
        scanner.run();
    }

    @Test
    public void suspendableCallTest() {
        final String suspCallMethod = MyXXXClassB.class.getSimpleName() + ".foo(I)V";
        final Set<String> suspependables = new HashSet<>();
        scanner.getSuspenablesAndSupers(suspependables, null);
        for (String susp : suspependables) {
            if (susp.contains(suspCallMethod))
                return;
        }
        fail(suspCallMethod + " is not suspendable");
    }

    @Test
    public void superSuspendableCallTest() {
        final String suspCallMethod = MyXXXClassA.class.getSimpleName() + ".foo(L";
        final Set<String> suspependables = new HashSet<>();
        scanner.getSuspenablesAndSupers(suspependables, null);
        for (String susp : suspependables) {
            if (susp.contains(suspCallMethod))
                return;
        }
        fail(suspCallMethod + " is not suspendable");
    }

    @Test
    public void nonSuperSuspendableCallTest() {
        final String suspCallMethod = MyXXXClassA.class.getSimpleName() + ".foo()";
        final Set<String> suspependables = new HashSet<>();
        scanner.getSuspenablesAndSupers(suspependables, null);
        for (String susp : suspependables) {
            if (susp.contains(suspCallMethod))
                fail(susp + " should not be suspendable");
        }
    }

    @Test
    public void superNonSuspendableCallTest() {
        final String suspCallMethod = MyXXXClassA.class.getSimpleName() + ".bar(";
        final Set<String> suspependables = new HashSet<>();
        scanner.getSuspenablesAndSupers(suspependables, null);
        for (String susp : suspependables) {
            if (susp.contains(suspCallMethod))
                fail(suspCallMethod + " should not be suspendable");
        }
    }

    @Test
    public void superSuspendableTest() {
        final String superSuspMethod = MyXXXInterfaceA.class.getSimpleName() + ".foo";
        final Set<String> suspependableSupers = new HashSet<>();
        scanner.getSuspenablesAndSupers(null, suspependableSupers);
        for (String susp : suspependableSupers) {
            if (susp.contains(superSuspMethod))
                return;
        }
        fail(superSuspMethod + " is not super suspendable");
    }

    @Test
    public void suspendableFileByAntTaskTest() {
        String suspFile = AutoSuspendablesScannerTest.class.getClassLoader().getResource("META-INF/testSuspendables").getFile();
        SimpleSuspendableClassifier ssc = new SimpleSuspendableClassifier(suspFile);
        assertTrue(ssc.isSuspendable(MyXXXClassB.class.getName().replace(".", "/"), "foo", "(I)V"));
    }
    
    static interface MyXXXInterfaceA {
        // super suspendable
        void foo(int t);

        // doesn't have suspandable implementation
        void bar(int t);
    }

    static class MyXXXClassA {
        // suspendable
        void foo(MyXXXInterfaceA a) {
            a.foo(0);
        }

        // not suspendable
        void foo() {
            bar(null); // test that if foo->bar->foo->... doesn't cause infinite loop
        }

        // not suspendable
        void bar(MyXXXInterfaceA a) {
            a.bar(0);
            foo();
        }
    }

    static class MyXXXClassB implements MyXXXInterfaceA {
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
}
