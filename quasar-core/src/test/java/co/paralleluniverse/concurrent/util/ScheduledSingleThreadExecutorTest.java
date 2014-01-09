/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import static org.hamcrest.CoreMatchers.*;

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
        assertTrue("count: " + count, count > 30 && count < 75);
    }

    @Test
    public void testMultipleProducers() throws Exception {
        final int nThread = 5;
        final int nSchedules = 50000;

        final AtomicInteger counter2 = new AtomicInteger();
        final AtomicInteger counter = new AtomicInteger();

        for (int t = 0; t < nThread; t++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < nSchedules; i++) {
                            counter2.incrementAndGet();
                            exec.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    counter.incrementAndGet();
                                }
                            }, 0, TimeUnit.MILLISECONDS);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }).start();
        }

        Thread.sleep(3000);

        assertThat(counter2.get(), is(nThread * nSchedules));
        assertThat(counter.get(), is(nThread * nSchedules));
    }
}
