/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument.live.fibers.instrument;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.fibers.instrument.live.LiveInstrumentationTest;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author mam
 */
public final class Merge2Test extends LiveInstrumentationTest implements SuspendableRunnable {
    private static Strand.UncaughtExceptionHandler previousUEH;

    @BeforeClass
    public static void setupClass() {
        previousUEH = Fiber.getDefaultUncaughtExceptionHandler();
        Fiber.setDefaultUncaughtExceptionHandler((s, e) -> Exceptions.rethrow(e));
    }

    @AfterClass
    public static void afterClass() {
        // Restore
        Fiber.setDefaultUncaughtExceptionHandler(previousUEH);
    }

    private interface Interface {
        void method();
    }

    private static Interface getInterface() {
        return null;
    }

    private static void suspendable() {
    }

    @Override
    public final void run() {
        try {
            final Interface iface = getInterface();
            //noinspection ConstantConditions
            iface.method();
        } catch (final IllegalStateException ise) {
            suspendable();
            assertThat(LiveInstrumentation.fetchRunCount(), equalTo(0L));
        }
    }

    @Test
    public final void testMerge2() {
        try {
            final Fiber c = new Fiber((String)null, null, new Merge2Test());
            TestsHelper.exec(c);
            assertTrue("Should not reach here", false);
        } catch (final NullPointerException ex) {
            // NPE expected
        }
    }
}
