package co.paralleluniverse.fibers.dynamic;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

import java.util.ArrayList;

public class DynamicallyLoadedFiber extends Fiber<ArrayList<String>> {
    @Override
    protected ArrayList<String> run() throws SuspendExecution, InterruptedException {
        ArrayList<String> results = new ArrayList<>();
        Test testClass = new Test();
        results.add("a");
        Fiber.park();
        results.add("b");
        testClass.test(results);
        results.add("c");
        return results;
    }

    private static class Base {
        public void baseTest(ArrayList<String> results) throws SuspendExecution {
            results.add("base1");
            Fiber.park();
            results.add("base2");
        }
    }

    private static class Test extends Base {
        public void test(ArrayList<String> results) throws SuspendExecution {
            results.add("o1");
            Fiber.park();
            results.add("o2");
            baseTest(results);
            results.add("o3");
        }
    }
}
