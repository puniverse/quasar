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
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.*;

public class ChannelSubscriberBlackboxTest extends SubscriberBlackboxVerification<Integer> {
    public static final long DEFAULT_TIMEOUT_MILLIS = 300L;

    private final int buffer;
    private final OverflowPolicy overflowPolicy;
    private final boolean batch;

    @Factory(dataProvider = "params")
    public ChannelSubscriberBlackboxTest(int buffer, OverflowPolicy overflowPolicy, boolean batch) {
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
//            {-1, OverflowPolicy.THROW, true},   // TCK bug
//            {-1, OverflowPolicy.THROW, false},   // TCK bug
//            {5, OverflowPolicy.DISPLACE, true}, // TCK bug
//            {5, OverflowPolicy.DISPLACE, false}, // TCK bug
            {1, OverflowPolicy.BLOCK, true},
            {1, OverflowPolicy.BLOCK, false},
            {1, OverflowPolicy.THROW, true},
            {1, OverflowPolicy.THROW, false}
        };
    }

    @Override
    public Subscriber<Integer> createSubscriber() {
        return new ChannelSubscriber<>(Channels.<Integer>newChannel(buffer, overflowPolicy, true, true), batch);
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }

    @Test
    public void testNothing() {
    }
}
