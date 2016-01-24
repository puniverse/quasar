/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.instrument.live.LiveInstrumentationTest;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;

import static co.paralleluniverse.fibers.TestsHelper.exec;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 *
 * @author mam
 */
//@Ignore
public final class InheritTest extends LiveInstrumentationTest {
    @Test
    public final void testInherit() {
        final C dut = new C();
        final Fiber c = new Fiber((String)null, null, (SuspendableRunnable) dut::myMethod);
        for(int i=0 ; i<3 ; i++) {
            exec(c);
        }
        
        assertEquals(5, dut.result.size());
        assertEquals("a", dut.result.get(0));
        assertEquals("o1", dut.result.get(1));
        assertEquals("o2", dut.result.get(2));
        assertEquals("b", dut.result.get(3));
        assertEquals("b", dut.result.get(4));

        assertThat(LiveInstrumentation.fetchRunCount(), equalTo(2L));
    }
    
    private static class A {
        static void suspend() {
            Fiber.park();
        }
    }

    private static class B extends A {
        final ArrayList<String> result = new ArrayList<>();
    }
    
    private static final class C extends B {
        public final void otherMethod() {
            result.add("o1");
            Fiber.park();
            result.add("o2");
        }
        
        public final void myMethod() {
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
