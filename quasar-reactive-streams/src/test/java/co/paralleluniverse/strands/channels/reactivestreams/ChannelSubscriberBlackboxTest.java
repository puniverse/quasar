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

import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.*;

public class ChannelSubscriberBlackboxTest extends SubscriberBlackboxVerification<Integer> {
    public static final long DEFAULT_TIMEOUT_MILLIS = 300L;

    private final int buffer;
    private final OverflowPolicy overflowPolicy;

    @Factory(dataProvider = "params")
    public ChannelSubscriberBlackboxTest(int buffer, OverflowPolicy overflowPolicy) {
        super(new TestEnvironment());
        // super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS));

        this.buffer = buffer;
        this.overflowPolicy = overflowPolicy;
    }

    @DataProvider(name = "params")
    public static Object[][] data() {
        return new Object[][]{
            {5, OverflowPolicy.THROW},
            {5, OverflowPolicy.BLOCK},
//            {-1, OverflowPolicy.THROW},   // TCK bug
//            {5, OverflowPolicy.DISPLACE}, // TCK bug
            {1, OverflowPolicy.BLOCK},
            {1, OverflowPolicy.THROW}
        };
    }

    @Override
    public Subscriber<Integer> createSubscriber() {
        return new ChannelSubscriber<>(buffer, overflowPolicy);
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }

    @Test
    public void testNothing() {
    }
}
