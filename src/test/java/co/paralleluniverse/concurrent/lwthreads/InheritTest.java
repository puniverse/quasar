/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.concurrent.lwthreads;

import java.util.ArrayList;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author mam
 */
public class InheritTest {

    @Test
    public void testInherit() {
        final C dut = new C();
        LightweightThread c = new LightweightThread(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                dut.myMethod();
            }
        });
        for(int i=0 ; i<3 ; i++) {
            c.exec();
        }
        
        assertEquals(5, dut.result.size());
        assertEquals("a", dut.result.get(0));
        assertEquals("o1", dut.result.get(1));
        assertEquals("o2", dut.result.get(2));
        assertEquals("b", dut.result.get(3));
        assertEquals("b", dut.result.get(4));
    }
    
    public static class A {
        public static void suspend() throws SuspendExecution {
            LightweightThread.suspend();
        }
    }
    
    public static class B extends A {
        final ArrayList<String> result = new ArrayList<String>();
    }
    
    public static class C extends B {
        
        public void otherMethod() throws SuspendExecution {
            result.add("o1");
            LightweightThread.suspend();
            result.add("o2");
        }
        
        public void myMethod() throws SuspendExecution {
            result.add("a");
            otherMethod();
            
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
