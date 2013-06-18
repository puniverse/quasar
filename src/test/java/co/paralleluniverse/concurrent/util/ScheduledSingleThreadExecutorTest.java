/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pron
 */
public class ScheduledSingleThreadExecutorTest {
    private ScheduledSingleThreadExecutor exec;
    
    public ScheduledSingleThreadExecutorTest() {
    }
    
    @Before
    public void setUp() {
        exec = new ScheduledSingleThreadExecutor();
    }
    
    @After
    public void tearDown() {
        exec.shutdown();
    }

    @Test
    public void testFixedRate() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        
        exec.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                counter.incrementAndGet();
            }
        }, 0, 30, TimeUnit.MILLISECONDS);
        
        Thread.sleep(2000);
        
        final int count = counter.get();
        assertTrue("count: " + count, count > 60 && count < 75);
    }
}
