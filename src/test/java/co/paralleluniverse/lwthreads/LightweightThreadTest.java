/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads;

import co.paralleluniverse.common.util.Exceptions;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pron
 */
public class LightweightThreadTest {
    private ForkJoinPool fjPool;

    public LightweightThreadTest() {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    @BeforeClass
    public static void setUpClass() {
        LightweightThread.setDefaultUncaughtExceptionHandler(new LightweightThread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(LightweightThread lwt, Throwable e) {
                Exceptions.rethrow(e);
            }
        });
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testTimeout() throws Exception {
        LightweightThread lwt = new LightweightThread(fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                LightweightThread.park(100, TimeUnit.MILLISECONDS);
            }
        }).start();

        
        try {
            lwt.join(2, TimeUnit.MILLISECONDS);
            fail();
        } catch (java.util.concurrent.TimeoutException e) {
        }

        lwt.join(200, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testInterrupt() throws Exception {
        LightweightThread lwt = new LightweightThread(fjPool, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                try {
                    LightweightThread.sleep(100);
                    fail("InterruptedException not thrown");
                } catch (LwtInterruptedException e) {
                }
            }
        });
        lwt.start();

        Thread.sleep(20);
        lwt.interrupt();
    }
}
