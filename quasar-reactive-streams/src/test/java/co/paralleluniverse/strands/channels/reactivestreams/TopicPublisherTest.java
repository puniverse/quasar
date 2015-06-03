/*
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.strands.channels.reactivestreams;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.Topic;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.*;

public class TopicPublisherTest extends PublisherVerification<Integer> {
    private static final long DEFAULT_TIMEOUT_MILLIS = 300L;
    private static final long PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS = 500L;
    private static final int DELAY_AMOUNT = 20;

    private final boolean delay;

    @Factory(dataProvider = "params")
    public TopicPublisherTest(boolean delay) {
        // super(new TestEnvironment());
        super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS), PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS);

        this.delay = delay;
    }

    @DataProvider(name = "params")
    public static Object[][] data() {
        return new Object[][]{{true}};
    }

    @Override
    public long maxElementsFromPublisher() {
        return Long.MAX_VALUE - 1;
    }

    @Override
    public long boundedDepthOfOnNextAndRequestRecursion() {
        return 1;
    }

    @Override
    public Publisher<Integer> createPublisher(final long elements) {
        final Topic<Integer> t = new Topic<>();

        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    if (delay)
                        Strand.sleep(DELAY_AMOUNT);
                    // we only emit up to 100K elements or 100ms, the later of the two (the TCK asks for 2^31-1)
                    long start = elements > 100_000 ? System.nanoTime() : 0L;
                    for (long i = 0; i < elements; i++) {
                        t.send((int) (i % 10000));

                        if (start > 0) {
                            long elapsed = (System.nanoTime() - start) / 1_000_000;
                            if (elapsed > 100)
                                break;
                        }
                    }
                    t.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw t;
                }
            }
        }).start();

        return ReactiveStreams.toPublisher(t);
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        final Topic<Integer> t = new Topic<>();

        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                try {
                    if (delay)
                        Strand.sleep(DELAY_AMOUNT);
                    t.close(new Exception("failure"));
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw t;
                }
            }
        }).start();

        return ReactiveStreams.toPublisher(t);
    }

    @Test
    public void testNothing() {
    }
}
