/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.Set;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

public class AutomaticSuspendablesScannerTest {
    private static Set<String> susps;

    @BeforeClass
    public static void buildGraph() {
        AutomaticSuspendablesScanner scanner = new AutomaticSuspendablesScanner(AutomaticSuspendablesScannerTest.class.getClassLoader());
        susps = scanner.findSuspendables();
    }

    @Test
    public void suspendableCallTest() {
        final String suspCallMethod = MyXXXClassB.class.getSimpleName() + ".foo(I)V";
        for (String susp : susps)
            if (susp.contains(suspCallMethod))
                return;
        fail(suspCallMethod +" is not suspendable");
    }

    @Test
    public void superSuspendableCallTest() {
        final String suspCallMethod = MyXXXClassA.class.getSimpleName() + ".foo(L"; // foo with class parameter
        for (String susp : susps)
            if (susp.contains(suspCallMethod)) {
                System.out.println("superSuspendableCallTest "+susp);
                return;
            }
        fail(suspCallMethod +" is not suspendable");
    }

    @Test
    public void nonSuperSuspendableCallTest() {
        final String suspCallMethod = MyXXXClassA.class.getSimpleName() + ".foo()"; // foo without parameters
        for (String susp : susps)
            if (susp.contains(suspCallMethod))
                fail(susp +" should not be suspendable");
    }

    @Test
    public void superNonSuspendableCallTest() {
        final String suspCallMethod = MyXXXClassA.class.getSimpleName() + ".bar(";
        for (String susp : susps)
            if (susp.contains(suspCallMethod))
                fail(suspCallMethod +" should not be suspendable");
    }

    static interface MyXXXInterfaceA {
        // has suspandable implementation
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
        }

        // not suspendable
        void bar(MyXXXInterfaceA a) {
            a.bar(0);
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

    public AutomaticSuspendablesScannerTest() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
}
