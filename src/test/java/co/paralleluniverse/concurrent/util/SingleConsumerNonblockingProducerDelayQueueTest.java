/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author pron
 */
public class SingleConsumerNonblockingProducerDelayQueueTest {
    public SingleConsumerNonblockingProducerDelayQueueTest() {
    }
    BlockingQueue<DelayedValue> q;

    @Before
    public void setUp() {
        q = new SingleConsumerNonblockingProducerDelayQueue<DelayedValue>();
    }

    @Test
    public void testPoll() throws Exception {
        q.offer(new DelayedValue(3, 150));
        q.offer(new DelayedValue(1, 50));
        q.offer(new DelayedValue(2, 100));

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

        Thread.sleep(20);
        dv = q.poll();
        assertThat(dv, is(nullValue()));

        Thread.sleep(30);
        dv = q.poll();
        assertThat(dv.getValue(), is(3));
    }

    @Test
    public void testTimedPoll() throws Exception {
        q.offer(new DelayedValue(2, 100));
        q.offer(new DelayedValue(1, 50));
        q.offer(new DelayedValue(3, 150));

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

        dv = q.poll(30, TimeUnit.MILLISECONDS);
        assertThat(dv.getValue(), is(3));
    }

    @Test
    public void testTake() throws Exception {
        q.offer(new DelayedValue(2, 100));
        q.offer(new DelayedValue(1, 50));
        q.offer(new DelayedValue(3, 150));

        DelayedValue dv;

        final long start = System.nanoTime();
        
        dv = q.take();
        assertThat(dv.getValue(), is(1));
        dv = q.take();
        assertThat(dv.getValue(), is(2));
        dv = q.take();
        assertThat(dv.getValue(), is(3));

        final long elapsedMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        
        assertTrue("elapsed: " + elapsedMillis, elapsedMillis > 140 && elapsedMillis < 170);
    }
}
