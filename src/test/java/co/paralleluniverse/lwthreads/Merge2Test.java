/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads;

import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendableRunnable;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author mam
 */
public class Merge2Test implements SuspendableRunnable {

    public interface Interface {
        public void method();
    }

    public static Interface getInterface() {
        return null;
    }

    public static void suspendable() throws SuspendExecution {
    }

    @Override
    public void run() throws SuspendExecution {
        try {
            Interface iface = getInterface();
            iface.method();
        } catch(IllegalStateException ise) {
            suspendable();
        }
    }

    @Test
    public void testMerge2() {
        try {
            LightweightThread c = new LightweightThread(new Merge2Test());
            c.exec();
            assertTrue("Should not reach here", false);
        } catch (NullPointerException ex) {
            // NPE expected
        }
    }
}
