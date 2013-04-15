/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author pron
 */
public class FiberTest {
    private ForkJoinPool fjPool;

    public FiberTest() {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    @BeforeClass
    public static void setUpClass() {
        Fiber.setDefaultUncaughtExceptionHandler(new Fiber.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Fiber lwt, Throwable e) {
                Exceptions.rethrow(e);
            }
        });
    }

    @Test
    public void testTimeout() throws Exception {
        Fiber fiber = new Fiber(fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                Fiber.park(100, TimeUnit.MILLISECONDS);
            }
        }).start();


        try {
            fiber.join(2, TimeUnit.MILLISECONDS);
            fail();
        } catch (java.util.concurrent.TimeoutException e) {
        }

        fiber.join(200, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testInterrupt() throws Exception {
        Fiber fiber = new Fiber(fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                try {
                    Fiber.sleep(100);
                    fail("InterruptedException not thrown");
                } catch (FiberInterruptedException e) {
                }
            }
        });
        fiber.start();

        Thread.sleep(20);
        fiber.interrupt();

        fiber.join();
    }

    @Test
    public void testThreadLocals() throws Exception {
        final ThreadLocal<String> tl1 = new ThreadLocal<>();
        final InheritableThreadLocal<String> tl2 = new InheritableThreadLocal<>();
        tl1.set("foo");
        tl2.set("bar");

        Fiber fiber = new Fiber(fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                assertThat(tl1.get(), is(nullValue()));
                assertThat(tl2.get(), is("bar"));

                tl1.set("koko");
                tl2.set("bubu");

                assertThat(tl1.get(), is("koko"));
                assertThat(tl2.get(), is("bubu"));

                Fiber.sleep(100);

                assertThat(tl1.get(), is("koko"));
                assertThat(tl2.get(), is("bubu"));
            }
        });
        fiber.start();
        fiber.join();
        
        assertThat(tl1.get(), is("foo"));
        assertThat(tl2.get(), is("bar"));
    }
}
