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

import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.channels.ReceivePort;
import co.paralleluniverse.strands.channels.Topic;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Converts between Quasar channels and reactive streams
 * @author pron
 */
public class ReactiveStreams {
    /**
     * Subscribes to a given {@link Publisher} and return a {@link ReceivePort} to the subscription.
     * This creates an internal <b>single consumer</b> channel that will receive the published elements.
     * 
     * @param bufferSize the size of the buffer of the internal channel; may be {@code -1} for unbounded, but may not be {@code 0})
     * @param policy     the {@link OverflowPolicy} of the internal channel.
     * @param batch      if the channel has a bounded buffer, whether to request further elements from the publisher in batches
     *                   whenever the channel's buffer is depleted, or after consuming each element.
     * @param publisher  the subscriber
     * @return A {@link ReceivePort} which emits the elements published by the subscriber
     */
    public static <T> ReceivePort<T> subscribe(int bufferSize, OverflowPolicy policy, boolean batch, Publisher<T> publisher) {
        final Channel<T> channel = Channels.newChannel(bufferSize, policy, true, true);
        final ChannelSubscriber<T> sub = new ChannelSubscriber<>(channel, batch);
        publisher.subscribe(sub);
        return sub;
    }

    /**
     * Turns a {@link ReceivePort channel} to a {@link Publisher}. All items sent to the channel will be published by
     * the publisher.
     * <p>
     * The publisher will allow a single subscription, unless the channel is a {@link Channels#isTickerChannel(ReceivePort) ticker channel}
     * in which case, multiple subscribers will be allowed, and a new {@link Channels#newTickerConsumerFor(Channel) ticker consumer}
     * will be created for each.
     * <p>
     * Every subscription to the returned publisher creates an internal fiber, that will receive items from the
     * channel and publish them.
     *
     * @param channel the channel
     * @param ff      the {@link FiberFactory} to create the internal fiber(s); if {@code null} then a default factory is used.
     * @return a new publisher for the channel's items
     */
    public static <T> Publisher<T> toPublisher(ReceivePort<T> channel, FiberFactory ff) {
        if (Channels.isTickerChannel(channel)) {
            return new ChannelPublisher<T>(ff, channel, false) {
                @Override
                protected ChannelSubscription<T> newChannelSubscription(Subscriber<? super T> s, Object channel) {
                    return super.newChannelSubscription(s, Channels.newTickerConsumerFor((Channel<T>) channel));
                }
            };
        } else
            return new ChannelPublisher<T>(ff, channel, true);
    }

    /**
     * Turns a {@link ReceivePort channel} to a {@link Publisher}. All items sent to the channel will be published by
     * the publisher.
     * <p>
     * The publisher will allow a single subscription, unless the channel is a {@link Channels#isTickerChannel(ReceivePort) ticker channel}
     * in which case, multiple subscribers will be allowed, and a new {@link Channels#newTickerConsumerFor(Channel) ticker consumer}
     * will be created for each.
     * <p>
     * Every subscription to the returned publisher creates an internal fiber, that will receive items from the
     * channel and publish them.
     * <p>
     * Calling this method is the same as calling {@link #toPublisher(ReceivePort, FiberFactory) toPublisher(channel, null)
     *
     * @param channel the channel
     * @return a new publisher for the channel's items
     */
    public static <T> Publisher<T> toPublisher(ReceivePort<T> channel) {
        return toPublisher(channel, null);
    }

    /**
     * Turns a {@link Topic topic} to a {@link Publisher}. All items sent to the topic will be published by
     * the publisher.
     * <p>
     * A new <i>transfer channel</i> (i.e. a blocking channel with a buffer of size 0) subscribed to the topic will be created for every subscriber.
     * <p>
     * Every subscription to the returned publisher creates an internal fiber, that will receive items from the
     * subscription's channel and publish them.
     *
     * @param topic the topic
     * @param ff    the {@link FiberFactory} to create the internal fiber(s); if {@code null} then a default factory is used.
     * @return a new publisher for the topic's items
     */
    public static <T> Publisher<T> toPublisher(Topic<T> topic, final FiberFactory ff) {
        return new ChannelPublisher<T>(ff, topic, false) {
            @Override
            protected ChannelSubscription<T> newChannelSubscription(Subscriber<? super T> s, Object channel) {
                final Topic<T> topic = (Topic<T>) channel;
                final Channel<T> ch = Channels.newChannel(0);
                try {
                    topic.subscribe(ch);
                    return new ChannelSubscription<T>(s, ch) {
                        @Override
                        public void cancel() {
                            super.cancel();
                            topic.unsubscribe(ch);
                        }
                    };
                } catch (Exception e) {
                    topic.unsubscribe(ch);
                    throw e;
                }
            }
        };
    }

    /**
     * Turns a {@link Topic topic} to a {@link Publisher}. All items sent to the topic will be published by
     * the publisher.
     * <p>
     * A new <i>transfer channel</i> (i.e. a blocking channel with a buffer of size 0) subscribed to the topic will be created for every subscriber.
     * <p>
     * Every subscription to the returned publisher creates an internal fiber, that will receive items from the
     * subscription's channel and publish them.
     * <p>
     * Calling this method is the same as calling {@link #toPublisher(ReceivePort, FiberFactory) toPublisher(channel, null)
     *
     * @param topic the topic
     * @return a new publisher for the topic's items
     */
    public static <T> Publisher<T> toPublisher(Topic<T> topic) {
        return toPublisher(topic, null);
    }
}
