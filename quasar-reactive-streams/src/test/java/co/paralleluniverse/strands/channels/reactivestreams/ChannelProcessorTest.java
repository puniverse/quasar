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
import co.paralleluniverse.strands.SuspendableAction2;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.channels.ReceivePort;
import co.paralleluniverse.strands.channels.SendPort;
import static co.paralleluniverse.strands.channels.reactivestreams.TestHelper.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.IdentityProcessorVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.*;

public class ChannelProcessorTest extends IdentityProcessorVerification<Integer> {
    private static final long DEFAULT_TIMEOUT_MILLIS = 300L;
    public static final long PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS = 1000L;

    private final OverflowPolicy overflowPolicy;
    private final boolean batch;

    @Factory(dataProvider = "params")
    public ChannelProcessorTest(OverflowPolicy overflowPolicy, boolean batch) {
//        super(new TestEnvironment());
        super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS), PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS);

        this.overflowPolicy = overflowPolicy;
        this.batch = batch;
    }

    @DataProvider(name = "params")
    public static Object[][] data() {
        return new Object[][]{
            {OverflowPolicy.BLOCK, false} // to save time, no need to run other configurations
        // {OverflowPolicy.BLOCK, true},
        // {OverflowPolicy.THROW, false},
        // {OverflowPolicy.THROW, true}
        };
    }

    @Override
    public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
        return ReactiveStreams.toProcessor(null, bufferSize, overflowPolicy, batch, new SuspendableAction2<ReceivePort<Integer>, SendPort<Integer>>() {

            @Override
            public void call(ReceivePort<Integer> in, SendPort<Integer> out) throws SuspendExecution, InterruptedException {
                for (Integer element; ((element = in.receive()) != null);) {
                    out.send(element);
                    Fiber.sleep(10); // just for fun
                }
                out.close();
            }
        });
    }

    @Override
    public long maxSupportedSubscribers() {
        return 1;
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return createDummyFailedPublisher();
    }

    @Test
    public void testNothing() {
    }

    @Override
    public ExecutorService publisherExecutorService() {
        return Executors.newFixedThreadPool(3);
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
