/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Test;

import java.util.ArrayList;

import static co.paralleluniverse.fibers.TestsHelper.exec;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author mam
 */
public final class InheritTest {
    @Test
    public void testInherit() {
        final C dut = new C();
        Fiber c = new Fiber((String)null, null, (SuspendableRunnable) dut::myMethod);
        for(int i=0 ; i<3 ; i++) {
            exec(c);
        }
        
        assertEquals(5, dut.result.size());
        assertEquals("a", dut.result.get(0));
        assertEquals("o1", dut.result.get(1));
        assertEquals("o2", dut.result.get(2));
        assertEquals("b", dut.result.get(3));
        assertEquals("b", dut.result.get(4));
    }
    
    private static class A {
        static void suspend() throws SuspendExecution {
            Fiber.park();
        }
    }

    private static class B extends A {
        final ArrayList<String> result = new ArrayList<>();
    }
    
    private static final class C extends B {
        public void otherMethod() throws SuspendExecution {
            result.add("o1");
            Fiber.park();
            result.add("o2");
        }
        
        public void myMethod() throws SuspendExecution {
            result.add("a");
            otherMethod();

            //noinspection InfiniteLoopStatement
            for(;;) {
                result.add("b");
                if(result.size() > 10) {
                    otherMethod();
                    result.add("Ohh!");
                }
                suspend();
            }
        }
    }
}
