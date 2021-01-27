/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.TestsHelper;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mam
 */
public class Merge2Test implements SuspendableRunnable {
    private static Strand.UncaughtExceptionHandler previousUEH;

    @BeforeClass
    public static void setupClass() {
        previousUEH = Fiber.getDefaultUncaughtExceptionHandler();
        Fiber.setDefaultUncaughtExceptionHandler(new Strand.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Strand s, Throwable e) {
                Exceptions.rethrow(e);
            }
        });
    }

    @AfterClass
    public static void afterClass() {
        // Restore
        Fiber.setDefaultUncaughtExceptionHandler(previousUEH);
    }

    public interface Interface {
        public void method();
    }

    public static Interface getInterface() {
        return null;
    }

    public static void suspendable() throws SuspendExecution {
    }

    @Override
    public void run() throws SuspendExecution {
        try {
            Interface iface = getInterface();
            iface.method();
        } catch(IllegalStateException ise) {
            suspendable();
        }
    }

    @Test
    public void testMerge2() {
        try {
            Fiber c = new Fiber((String)null, null, new Merge2Test());
            TestsHelper.exec(c);
            assertTrue("Should not reach here", false);
        } catch (NullPointerException ex) {
            // NPE expected
        }
    }
}
