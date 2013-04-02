/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.lwthreads;

import static org.junit.Assert.*;
import org.junit.Test;


/**
 *
 * @author Matthias Mann
 */
public class DoubleTest implements SuspendableRunnable {

    double result;

    @Test
    public void testDouble() {
        LightweightThread co = new LightweightThread(this);
        co.exec();
        assertEquals(0, result, 1e-8);
        boolean res = co.exec();
        assertEquals(1, result, 1e-8);
        assertEquals(res, true);
    }

    @Override
    public void run() throws SuspendExecution {
        double temp = Math.cos(0);
        LightweightThread.park();
        this.result = temp;
    }

}
