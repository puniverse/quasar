/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Matthias Mann
 */
public final class UninitializedTest implements SuspendableRunnable {
    private Object result = "b";

    @Test
    public final void testUninitialized() {
        Fiber co = new Fiber((String) null, null, this);
        int count = 1;
        while (!TestsHelper.exec(co))
            count++;

        assertEquals(2, count);
        assertEquals("a", result);
    }

    @Override
    public final void run() {
        result = getProperty();
    }

    private Object getProperty() {
        Object x;

        final Object y = getProperty("a");
        if (y != null) {
            //noinspection SuspiciousNameCombination
            x = y;
        } else {
            x = getProperty("c");
        }

        return x;
    }

    private Object getProperty(String string) {
        Fiber.park();
        return string;
    }
}
