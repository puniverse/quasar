/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads;

import java.lang.reflect.Field;
import static org.junit.Assert.*;
import org.junit.Test;


/**
 *
 * @author Matthias Mann
 */
public class InitialSizeTest implements SuspendableRunnable {
    
    @Test
    public void test1() {
        testWithSize(1);
    }
    
    @Test
    public void test2() {
        testWithSize(2);
    }
    
    @Test
    public void test3() {
        testWithSize(3);
    }
    
    private void testWithSize(int stackSize) {
        LightweightThread c = new LightweightThread(this, stackSize);
        assertEquals(getStackSize(c), stackSize);
        boolean res = c.exec();
        assertEquals(res, false);
        res = c.exec();
        assertEquals(res, true);
        assertTrue(getStackSize(c) > 10);
    }

    @Override
    public void run() throws SuspendExecution {
        assertEquals(3628800, factorial(10));
    }
    
    private int factorial(Integer a) throws SuspendExecution {
        if(a == 0) {
            LightweightThread.park();
            return 1;
        }
        return a * factorial(a - 1);
    }
    
    private int getStackSize(LightweightThread c) {
        try {
            Field stackField = LightweightThread.class.getDeclaredField("stack");
            stackField.setAccessible(true);
            Object stack = stackField.get(c);
            Field dataObjectField = Stack.class.getDeclaredField("dataObject");
            dataObjectField.setAccessible(true);
            Object[] dataObject = (Object[])dataObjectField.get(stack);
            return dataObject.length;
        } catch(Throwable ex) {
            throw new AssertionError(ex);
        }
    }
}
