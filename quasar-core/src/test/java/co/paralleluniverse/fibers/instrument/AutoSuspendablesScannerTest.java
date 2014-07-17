package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.net.URLClassLoader;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

public class AutoSuspendablesScannerTest {
    private static AutoSuspendablesScanner scanner;

    @BeforeClass
    public static void buildGraph() {
        scanner = new AutoSuspendablesScanner((URLClassLoader) AutoSuspendablesScannerTest.class.getClassLoader());
    }

    @Test
    public void suspendableCallTest() {
        final String suspCallMethod = MyXXXClassB.class.getSimpleName() + ".foo(I)V";
        for (String susp : scanner.getSuspendables())
            if (susp.contains(suspCallMethod))
                return;
        fail(suspCallMethod + " is not suspendable");
    }

    @Test
    public void superSuspendableCallTest() {
        final String suspCallMethod = MyXXXClassA.class.getSimpleName() + ".foo(L";
        for (String susp : scanner.getSuspendables())
            if (susp.contains(suspCallMethod))
                return;
        fail(suspCallMethod + " is not suspendable");
    }

    @Test
    public void nonSuperSuspendableCallTest() {
        final String suspCallMethod = MyXXXClassA.class.getSimpleName() + ".foo()";
        for (String susp : scanner.getSuspendables())
            if (susp.contains(suspCallMethod))
                fail(susp + " should not be suspendable");
    }

    @Test
    public void superNonSuspendableCallTest() {
        final String suspCallMethod = MyXXXClassA.class.getSimpleName() + ".bar(";
        for (String susp : scanner.getSuspendables())
            if (susp.contains(suspCallMethod))
                fail(suspCallMethod + " should not be suspendable");
    }

    @Test
    public void superSuspendableTest() {
        final String superSuspMethod = MyXXXInterfaceA.class.getSimpleName() + ".foo";
        for (String susp : scanner.getSuperSuspendables())
            if (susp.contains(superSuspMethod))
                return;
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
