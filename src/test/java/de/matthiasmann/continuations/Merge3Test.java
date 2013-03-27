/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.matthiasmann.continuations;

import junit.framework.TestCase;

/**
 *
 * @author Matthias Mann
 */
public class Merge3Test extends TestCase implements CoroutineProto {

    public boolean a;
    public boolean b;
    
    public void coExecute() throws SuspendExecution {
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
    
    @org.junit.Test
    public void testMerge3() {
        Coroutine c = new Coroutine(new Merge3Test());
        c.run();
    }
}
