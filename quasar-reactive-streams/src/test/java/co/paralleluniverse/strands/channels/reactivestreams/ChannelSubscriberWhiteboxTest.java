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

import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.*;

public class ChannelSubscriberWhiteboxTest extends SubscriberWhiteboxVerification<Integer> {
    public static final long DEFAULT_TIMEOUT_MILLIS = 300L;

    private final int buffer;
    private final OverflowPolicy overflowPolicy;
    private final boolean batch;

    @Factory(dataProvider = "params")
    public ChannelSubscriberWhiteboxTest(int buffer, OverflowPolicy overflowPolicy, boolean batch) {
        super(new TestEnvironment());
        // super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS));

        this.buffer = buffer;
        this.overflowPolicy = overflowPolicy;
        this.batch = batch;
    }

    @DataProvider(name = "params")
    public static Object[][] data() {
        return new Object[][]{
            {5, OverflowPolicy.THROW, true},
            {5, OverflowPolicy.THROW, false},
            {5, OverflowPolicy.BLOCK, true},
            {5, OverflowPolicy.BLOCK, false},
            {-1, OverflowPolicy.THROW, true},
            {-1, OverflowPolicy.THROW, false},
            {5, OverflowPolicy.DISPLACE, true},
            {5, OverflowPolicy.DISPLACE, false},
//            {1, OverflowPolicy.BLOCK, true},  // TCK bug
//            {1, OverflowPolicy.BLOCK, false}, // TCK bug
//            {1, OverflowPolicy.THROW, true},  // TCK bug
//            {1, OverflowPolicy.THROW, false}  // TCK bug
        };
    }

    @Override
    public Subscriber<Integer> createSubscriber(final WhiteboxSubscriberProbe<Integer> probe) {
        return new ChannelSubscriber<Integer>(Channels.<Integer>newChannel(buffer, overflowPolicy, true, true), batch) {

            @Override
            public void onSubscribe(final Subscription s) {
                super.onSubscribe(s);
                probe.registerOnSubscribe(new SubscriberPuppet() {
                    @Override
                    public void triggerRequest(long elements) {
                        s.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        s.cancel();
                    }
                });
            }

            @Override
            public void onNext(Integer element) {
                super.onNext(element);
                probe.registerOnNext(element);
            }

            @Override
            public void onError(Throwable cause) {
                super.onError(cause);
                probe.registerOnError(cause);
            }

            @Override
            public void onComplete() {
                super.onComplete();
                probe.registerOnComplete();
            }
        };
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }

    @Test
    public void testNothing() {
    }
}
