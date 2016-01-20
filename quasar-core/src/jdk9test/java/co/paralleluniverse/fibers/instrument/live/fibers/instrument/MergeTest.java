/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.fibers.instrument.live.LiveInstrumentationTest;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 *
 * @author mam
 */
public class MergeTest extends LiveInstrumentationTest implements SuspendableRunnable {
    public static void throwsIO() throws IOException {
    }

    @Override
    public final void run() {
        try {
            throwsIO();
        } catch(final IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public final void testMerge() {
        Fiber c = new Fiber<>((String)null, null, new MergeTest());
        TestsHelper.exec(c);
        assertThat(LiveInstrumentation.fetchRunCount(), equalTo(0L));
    }
}
