/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.Stack;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.fibers.instrument.live.LiveInstrumentationTest;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

/**
 * @author Matthias Mann
 */
//@Ignore
public final class InitialSizeTest extends LiveInstrumentationTest implements SuspendableRunnable {
    
    @Test
    public final void test1() {
        testWithSize(1);
    }
    
    @Test
    public final void test2() {
        testWithSize(2);
    }
    
    @Test
    public final void test3() {
        testWithSize(3);
    }
    
    private void testWithSize(int stackSize) {
        Fiber c = new Fiber(null, null, stackSize, this);
        //assertEquals(getStackSize(c), stackSize);
        boolean res = TestsHelper.exec(c);
        assertEquals(false, res);
        res = TestsHelper.exec(c);
        assertEquals(true, res);
        assertTrue(getStackSize(c) > 10);

        assertThat(LiveInstrumentation.fetchRunCount(), equalTo(1L));
    }

    @Override
    public final void run() {
        int fac10 = factorial(10);
        assertEquals(3628800, fac10);
    }
    
    private int factorial(Integer a) {
        if (a == 0) {
            Fiber.park();
            return 1;
        }
        return a * factorial(a - 1);
    }
    
    private int getStackSize(Fiber c) {
        try {
            final Field stackField = Fiber.class.getDeclaredField("stack");
            stackField.setAccessible(true);
            final Object stack = stackField.get(c);
            final Field dataObjectField = Stack.class.getDeclaredField("dataObject");
            dataObjectField.setAccessible(true);
            final Object[] dataObject = (Object[])dataObjectField.get(stack);
            return dataObject.length;
        } catch (final Throwable ex) {
            throw new AssertionError(ex);
        }
    }
}
