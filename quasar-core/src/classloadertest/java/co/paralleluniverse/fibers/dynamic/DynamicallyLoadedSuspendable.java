package co.paralleluniverse.fibers.dynamic;

import co.paralleluniverse.common.test.TestInterface;
import co.paralleluniverse.common.test.TestInterface2;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

import java.util.ArrayList;

public class DynamicallyLoadedSuspendable implements TestInterface {
    @Suspendable
    public void test(ArrayList<String> results) {
        results.add("a");
        TestInterface referencedSuspend = new ReferencedSuspendable();
        TestInterface2 indirectSuspend = new IndirectSuspendable();
        results.add("b");
        try {
            results.add("c");
            Fiber.park();
            results.add("d");
        } catch (SuspendExecution ex) {

        }
        results.add("e");
        referencedSuspend.test(results);
        results.add("f");
        indirectSuspend.test(results, referencedSuspend);
        results.add("g");
        while (true) {
            results.add("h");
            indirectSuspend.test(results, referencedSuspend);
            results.add("i");
        }
    }

    private static class BaseSuspendable {
        @Suspendable
        public void baseTest(ArrayList<String> results) {
            try {
                results.add("b1");
                Fiber.park();
                results.add("b2");
            } catch (SuspendExecution ex) {

            }
        }
    }

    private static class ReferencedSuspendable extends BaseSuspendable implements TestInterface {
        @Override
        @Suspendable
        public void test(ArrayList<String> results) {
            try {
                results.add("d1");
                Fiber.park();
                results.add("d2");
            } catch (SuspendExecution ex) {

            }
            baseTest(results);
        }
    }

    private static class IndirectSuspendable extends BaseSuspendable implements TestInterface2 {
        @Override
        @Suspendable
        public void test(ArrayList<String> results, TestInterface target) {
            results.add("o1");
            target.test(results);
            results.add("o2");
            baseTest(results);
        }
    }
}
