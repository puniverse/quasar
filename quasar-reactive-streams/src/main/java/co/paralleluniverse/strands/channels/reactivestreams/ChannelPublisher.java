/*
 * Quasar: lightweight threads and actors for the JVM.
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
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.ReceivePort;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 *
 * @author pron
 */
class ChannelPublisher<T> implements Publisher<T> {
    private final FiberFactory ff;
    private final Object channel;
    private final AtomicBoolean subscribed;

    public ChannelPublisher(FiberFactory ff, Object channel, boolean singleSubscriber) {
        this.ff = ff != null ? ff : defaultFiberFactory;
        this.channel = channel;

        subscribed = singleSubscriber ? new AtomicBoolean() : null;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        if (s == null)
            throw new NullPointerException(); // #1.9
        try {
            if (subscribed != null && !subscribed.compareAndSet(false, true))
                s.onError(new RuntimeException("already subscribed"));
            else
                ff.newFiber(newChannelSubscription(s, channel)).start();
        } catch (Exception e) {
            s.onError(e);
        }
    }

    protected ChannelSubscription<T> newChannelSubscription(Subscriber<? super T> s, Object channel) {
        return new ChannelSubscription<>(s, (ReceivePort<T>)channel);
    }

    private static final FiberFactory defaultFiberFactory = new FiberFactory() {
        @Override
        public <T> Fiber<T> newFiber(SuspendableCallable<T> target) {
            return new Fiber(target);
        }
    };
}
