/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.fibers.instrument.live.LiveInstrumentationTest;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Matthias Mann
 */
//@Ignore
public final class DoubleTest extends LiveInstrumentationTest implements SuspendableRunnable {
    private double result;

    @Test @Ignore // TODO: Re-enable when double numbers work
    public final void testDouble() {
        Fiber co = new Fiber<>((String)null, null, this);
        TestsHelper.exec(co);
        assertEquals(0, result, 1e-8);
        boolean res = TestsHelper.exec(co);
        assertEquals(1, result, 1e-8);
        assertEquals(res, true);

        assertThat(LiveInstrumentation.fetchRunCount(), equalTo(1L));
    }

    @Override
    public final void run() throws SuspendExecution {
        double temp = Math.cos(0);
        Fiber.park();
        this.result = temp;
    }
}
