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

import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.common.util.Debug;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.CoreMatchers.*;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

/**
 *
 * @author pron
 */
public class SingleConsumerNonblockingProducerDelayQueueTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;

    private static final boolean SEQUENCED = false;

    public SingleConsumerNonblockingProducerDelayQueueTest() {
    }
    BlockingQueue<DelayedValue> q;

    @Before
    public void setUp() {
        q = new SingleConsumerNonblockingProducerDelayQueue<>(); // new DelayQueue<>(); // 
    }

    @Test
    public void testPoll() throws Exception {
        q.offer(DelayedValue.instance(SEQUENCED, 3, 150));
        q.offer(DelayedValue.instance(SEQUENCED, 1, 50));
        q.offer(DelayedValue.instance(SEQUENCED, 2, 100));

        DelayedValue dv;

        Thread.sleep(30);
        dv = q.poll();
        assertThat(dv, is(nullValue()));

        Thread.sleep(30);
        dv = q.poll();
        assertThat(dv.getValue(), is(1));

        Thread.sleep(20);
        dv = q.poll();
        assertThat(dv, is(nullValue()));

        Thread.sleep(30);
        dv = q.poll();
        assertThat(dv.getValue(), is(2));

        Thread.sleep(15);
        dv = q.poll();
        assertThat(dv, is(nullValue()));

        Thread.sleep(40);
        dv = q.poll();
        assertThat(dv.getValue(), is(3));
    }

    @Test
    public void testTimedPoll() throws Exception {
        q.offer(DelayedValue.instance(SEQUENCED, 2, 100));
        q.offer(DelayedValue.instance(SEQUENCED, 1, 50));
        q.offer(DelayedValue.instance(SEQUENCED, 3, 150));

        DelayedValue dv;

        dv = q.poll(30, TimeUnit.MILLISECONDS);
        assertThat(dv, is(nullValue()));

        dv = q.poll(30, TimeUnit.MILLISECONDS);
        assertThat(dv.getValue(), is(1));

        dv = q.poll(20, TimeUnit.MILLISECONDS);
        assertThat(dv, is(nullValue()));

        dv = q.poll(30, TimeUnit.MILLISECONDS);
        assertThat(dv.getValue(), is(2));

        dv = q.poll(20, TimeUnit.MILLISECONDS);
        assertThat(dv, is(nullValue()));

        dv = q.poll(40, TimeUnit.MILLISECONDS);
        assertThat(dv.getValue(), is(3));
    }

    @Test
    public void testTake() throws Exception {
        q.offer(DelayedValue.instance(SEQUENCED, 2, 100));
        q.offer(DelayedValue.instance(SEQUENCED, 1, 50));
        q.offer(DelayedValue.instance(SEQUENCED, 3, 150));

        DelayedValue dv;

        final long start = System.nanoTime();

        dv = q.take();
        assertThat(dv.getValue(), is(1));
        dv = q.take();
        assertThat(dv.getValue(), is(2));
        dv = q.take();
        assertThat(dv.getValue(), is(3));

        final long elapsedMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);

        assertTrue("elapsed: " + elapsedMillis, elapsedMillis > 140 && elapsedMillis < 300);
    }

    @Test
    public void testTimedPollWithSurpriseInsertions() throws Exception {
        DelayedValue dv;

        dv = q.poll(30, TimeUnit.MILLISECONDS);
        assertThat(dv, is(nullValue()));

        q.offer(DelayedValue.instance(SEQUENCED, 2, 200));

        dv = q.poll(30, TimeUnit.MILLISECONDS);
        assertThat(dv, is(nullValue()));

        q.offer(DelayedValue.instance(SEQUENCED, 1, 20));

        dv = q.poll(30, TimeUnit.MILLISECONDS);
        assertThat(dv.getValue(), is(1));

        dv = q.poll(150, TimeUnit.MILLISECONDS);
        assertThat(dv.getValue(), is(2));
    }
}
