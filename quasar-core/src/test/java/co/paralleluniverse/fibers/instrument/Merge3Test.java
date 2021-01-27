/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Test;

/**
 *
 * @author Matthias Mann
 */
public class Merge3Test implements SuspendableRunnable {

    public boolean a;
    public boolean b;
    
    @Override
    public void run() throws SuspendExecution {
        if(a) {
            Object[] arr = new Object[2];
            System.out.println(arr);
        } else {
            float[] arr = new float[3];
            System.out.println(arr);
        }
        blub();
        System.out.println();
    }
    
    private void blub() throws SuspendExecution {
    }
    
    @Test
    public void testMerge3() {
        Fiber c = new Fiber((String)null, null, new Merge3Test());
        TestsHelper.exec(c);
    }
}
