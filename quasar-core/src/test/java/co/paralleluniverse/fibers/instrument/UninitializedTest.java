/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Matthias Mann
 */
public class UninitializedTest implements SuspendableRunnable {

    Object result = "b";

    @Test
    public void testUninitialized() {
        Fiber co = new Fiber((String) null, null, this);
        int count = 1;
        while (!TestsHelper.exec(co))
            count++;

        assertEquals(2, count);
        assertEquals("a", result);
    }

    @Override
    public void run() throws SuspendExecution {
        result = getProperty();
    }

    private Object getProperty() throws SuspendExecution {
        Object x;

        Object y = getProperty("a");
        if (y != null) {
            x = y;
        } else {
            x = getProperty("c");
        }

        return x;
    }

    private Object getProperty(String string) throws SuspendExecution {
        Fiber.park();
        return string;
    }

}
