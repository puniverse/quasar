package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static co.paralleluniverse.fibers.TestsHelper.exec;
import static org.junit.Assert.assertEquals;

public class SuspendableIteratorTest {

    private final List<String> results = new ArrayList<>();

    static class SuspendableIteratorImpl {
        List<String> elems = new ArrayList<>();
        {
            elems.add("A");
            elems.add("B");
            elems.add("C");
        }

        @Suspendable
        public String next() {
            try {
                Fiber.park();
            } catch (SuspendExecution e) {
                throw new AssertionError(e);
            }
            return elems.remove(0);
        }

        @Suspendable
        public boolean hasNext() {
            try {
                Fiber.park();
            } catch (SuspendExecution e) {
                throw new AssertionError(e);
            }
            return !elems.isEmpty();
        }
    };

    interface SuspendableIteratorInterface<T> extends Iterator<T> {
        @Override
        @Suspendable
        T next();

        @Override
        @Suspendable
        boolean hasNext();
    }

    static abstract class SuspendableIteratorClass<T> implements Iterator<T> {
        @Override
        @Suspendable
        public abstract T next();

        @Override
        @Suspendable
        public abstract boolean hasNext();
    }

    static class SuspendableListWithIteratorInterface implements Iterable<String> {
        @Override
        public SuspendableIteratorInterface<String> iterator() {
            return new SuspendableIteratorInterface<String>(){
                SuspendableIteratorImpl impl = new SuspendableIteratorImpl();

                @Override
                public void remove() {
                }

                @Suspendable
                @Override
                public String next() {
                    return impl.next();
                }

                @Suspendable
                @Override
                public boolean hasNext() {
                    return impl.hasNext();
                }
            };
        }
    }

    static class SuspendableListWithIteratorClass implements Iterable<String> {
        @Override
        public SuspendableIteratorClass<String> iterator() {
            return new SuspendableIteratorClass<String>(){
                SuspendableIteratorImpl impl = new SuspendableIteratorImpl();

                @Override
                public void remove() {
                }

                @Suspendable
                @Override
                public String next() {
                    return impl.next();
                }

                @Suspendable
                @Override
                public boolean hasNext() {
                    return impl.hasNext();
                }
            };
        }
    }

    @Suspendable
    private void suspendableListWithIteratorInterface(){
        SuspendableListWithIteratorInterface l = new SuspendableListWithIteratorInterface();
        for(String elem : l){
            results.add(elem);
        }
    }

    @Test
    public void testSuspendableListWithIteratorInterface(){
        Fiber co = new Fiber((String) null, null, (SuspendableCallable) null) {
            @Override
            protected Object run() throws SuspendExecution, InterruptedException {
                suspendableListWithIteratorInterface();
                return null;
            }
        };
        runTest(co);
    }

    @Suspendable
    private void suspendableListWithIteratorClass(){
        SuspendableListWithIteratorClass l = new SuspendableListWithIteratorClass();
        for(String elem : l){
            results.add(elem);
        }
    }

    @Test
    public void testSuspendableListWithIteratorClass(){
        Fiber co = new Fiber((String) null, null, (SuspendableCallable) null) {
            @Override
            protected Object run() throws SuspendExecution, InterruptedException {
                suspendableListWithIteratorClass();
                return null;
            }
        };
        runTest(co);
    }

    @Suspendable
    private void suspendableListWithIteratorClassMultiple(){
        SuspendableListWithIteratorClass l = new SuspendableListWithIteratorClass();
        for(String elem : l){
            results.add(elem);
            for(String elem2 : l){
                results.add(elem2);
            }
        }
        for(String elem : l){
            results.add(elem);
        }
    }

    @Test
    public void testSuspendableListWithIteratorClassMultiple(){
        Fiber co = new Fiber((String) null, null, (SuspendableCallable) null) {
            @Override
            protected Object run() throws SuspendExecution, InterruptedException {
                suspendableListWithIteratorClassMultiple();
                return null;
            }
        };
        runTest(co, 35, Arrays.asList("A", "A", "B", "C",
                "B", "A", "B", "C",
                "C", "A", "B", "C",
                "A", "B", "C"));
    }

    private void runTest(Fiber co) {
        runTest(co, 7, Arrays.asList("A", "B", "C"));
    }

    private void runTest(Fiber co, int iters, List<String> expected){
        try{
            for(int i=0;i<iters;i++)
                exec(co);
        } finally {
            System.out.println(results);
        }

        assertEquals(expected, results);
    }
}
