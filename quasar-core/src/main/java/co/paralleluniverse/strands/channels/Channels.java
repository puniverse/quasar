/*
 * Quasar: lightweight strands and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.common.util.Function2;
import co.paralleluniverse.common.util.Function3;
import co.paralleluniverse.common.util.Function4;
import co.paralleluniverse.common.util.Function5;
import co.paralleluniverse.strands.queues.ArrayQueue;
import co.paralleluniverse.strands.queues.BasicQueue;
import co.paralleluniverse.strands.queues.BasicSingleConsumerDoubleQueue;
import co.paralleluniverse.strands.queues.BasicSingleConsumerFloatQueue;
import co.paralleluniverse.strands.queues.BasicSingleConsumerIntQueue;
import co.paralleluniverse.strands.queues.BasicSingleConsumerLongQueue;
import co.paralleluniverse.strands.queues.BoxQueue;
import co.paralleluniverse.strands.queues.CircularDoubleBuffer;
import co.paralleluniverse.strands.queues.CircularFloatBuffer;
import co.paralleluniverse.strands.queues.CircularIntBuffer;
import co.paralleluniverse.strands.queues.CircularLongBuffer;
import co.paralleluniverse.strands.queues.CircularObjectBuffer;
import co.paralleluniverse.strands.queues.SingleConsumerArrayDoubleQueue;
import co.paralleluniverse.strands.queues.SingleConsumerArrayFloatQueue;
import co.paralleluniverse.strands.queues.SingleConsumerArrayIntQueue;
import co.paralleluniverse.strands.queues.SingleConsumerArrayLongQueue;
import co.paralleluniverse.strands.queues.SingleConsumerArrayObjectQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayDoubleQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayFloatQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayIntQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayLongQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayObjectQueue;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * A utility class for creating and manipulating channels.
 *
 * @author pron
 */
public final class Channels {
    /**
     * Determines how a channel behaves when its internal buffer (if it has one) overflows.
     */
    public enum OverflowPolicy {
        /**
         * The sender will get an exception (except if the channel is an actor's mailbox)
         */
        THROW,
        /**
         * The message will be silently dropped.
         */
        DROP,
        /**
         * The sender will block until there's a vacancy in the channel.
         */
        BLOCK,
        /**
         * The sender will block for some time, and retry.
         */
        BACKOFF,
        /**
         * The oldest message in the queue will be removed to make room for the new message.
         */
        DISPLACE
    }
    private static final OverflowPolicy defaultPolicy = OverflowPolicy.BLOCK;
    private static final boolean defaultSingleProducer = false;
    private static final boolean defaultSingleConsumer = true;

    /**
     * Creates a new channel with the given properties.
     * <p/>
     * Some combinations of properties are unsupported, and will throw an {@code IllegalArgumentException} if requested:
     *
     * <ul>
     * <li>unbounded channel with multiple consumers</li>
     * <li>a transfer channel with any overflow policy other than {@link OverflowPolicy#BLOCK BLOCK}</li>
     * <li>An overflow policy of {@link OverflowPolicy#DISPLACE DISPLACE} with multiple consumers.</li>
     * </ul>
     * An unbounded channel ignores its overflow policy as it never overflows.
     *
     *
     * @param <Message>      the type of messages that can be sent to this channel.
     * @param bufferSize     if positive, the number of messages that the channel can hold in an internal buffer;
     *                       {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                       {@code -1} for a channel with an unbounded (infinite) buffer.
     * @param policy         the {@link OverflowPolicy} specifying how the channel (if bounded) will behave if its internal buffer overflows.
     * @param singleProducer whether the channel will be used by a single producer strand.
     * @param singleConsumer whether the channel will be used by a single consumer strand.
     * @return The newly created channel
     */
    public static <Message> Channel<Message> newChannel(int bufferSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (bufferSize == 0) {
            if (policy != OverflowPolicy.BLOCK)
                throw new IllegalArgumentException("Cannot use policy " + policy + " for channel with size 0 (only BLOCK supported");
            return new TransferChannel<Message>();
        }

        final BasicQueue<Message> queue;
        if (bufferSize < 0) {
            if (!singleConsumer)
                throw new IllegalArgumentException("Unbounded queue with multiple consumers is unsupported");
            queue = new SingleConsumerLinkedArrayObjectQueue<Message>();
        } else if (bufferSize == 1)
            queue = new BoxQueue<Message>(policy == OverflowPolicy.DISPLACE, singleConsumer);
        else if (policy == OverflowPolicy.DISPLACE) {
            if (!singleConsumer)
                throw new IllegalArgumentException("Channel with DISPLACE policy configuration is not supported for multiple consumers");
            queue = new CircularObjectBuffer<Message>(bufferSize, singleProducer);
        } else if (singleConsumer)
            queue = new SingleConsumerArrayObjectQueue<Message>(bufferSize);
        else
            queue = new ArrayQueue<Message>(bufferSize);


        return new QueueObjectChannel(queue, policy, singleConsumer);
    }

    /**
     * Creates a new channel with the given mailbox size and {@link OverflowPolicy}, with other properties set to their default values.
     * Specifically, {@code singleProducer} will be set to {@code false}, while {@code singleConsumer} will be set to {@code true}.
     *
     * @param <Message>  the type of messages that can be sent to this channel.
     * @param bufferSize if positive, the number of messages that the channel can hold in an internal buffer;
     *                   {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                   {@code -1} for a channel with an unbounded (infinite) buffer.
     * @param policy     the {@link OverflowPolicy} specifying how the channel (if bounded) will behave if its internal buffer overflows.
     * @return The newly created channel
     * @see #newChannel(int, co.paralleluniverse.strands.channels.Channels.OverflowPolicy, boolean, boolean)
     */
    public static <Message> Channel<Message> newChannel(int bufferSize, OverflowPolicy policy) {
        return newChannel(bufferSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    /**
     * Creates a new channel with the given mailbox size with other properties set to their default values.
     * Specifically, the {@link OverflowPolicy} will be set to {@link OverflowPolicy#BLOCK BLOCK},
     * {@code singleProducer} will be set to {@code false}, and {@code singleConsumer} will be set to {@code true}.
     *
     * @param <Message>  the type of messages that can be sent to this channel.
     * @param bufferSize if positive, the number of messages that the channel can hold in an internal buffer;
     *                   {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                   {@code -1} for a channel with an unbounded (infinite) buffer.
     * @return The newly created channel
     * @see #newChannel(int, co.paralleluniverse.strands.channels.Channels.OverflowPolicy, boolean, boolean)
     */
    public static <Message> Channel<Message> newChannel(int bufferSize) {
        return newChannel(bufferSize, bufferSize == 0 ? OverflowPolicy.BLOCK : defaultPolicy);
    }

    /**
     * Creates a new primitive {@code int} channel with the given properties.
     * <p/>
     * Some combinations of properties are unsupported, and will throw an {@code IllegalArgumentException} if requested:
     *
     * <ul>
     * <li>multiple consumers</li>
     * <li>a transfer channel with any overflow policy other than {@link OverflowPolicy#BLOCK BLOCK}</li>
     * <li>An overflow policy of {@link OverflowPolicy#DISPLACE DISPLACE} with multiple consumers.</li>
     * </ul>
     * An unbounded channel ignores its overflow policy as it never overflows.
     *
     * @param bufferSize     if positive, the number of messages that the channel can hold in an internal buffer;
     *                       {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                       {@code -1} for a channel with an unbounded (infinite) buffer.
     * @param policy         the {@link OverflowPolicy} specifying how the channel (if bounded) will behave if its internal buffer overflows.
     * @param singleProducer whether the channel will be used by a single producer strand.
     * @param singleConsumer whether the channel will be used by a single consumer strand. Currently primitive channels only support a single
     *                       consumer, so this argument must be set to {@code false}.
     * @return The newly created channel
     */
    public static IntChannel newIntChannel(int bufferSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (!singleConsumer)
            throw new IllegalArgumentException("Primitive queue with multiple consumers is unsupported");

        final BasicSingleConsumerIntQueue queue;
        if (bufferSize < 0) {
            queue = new SingleConsumerLinkedArrayIntQueue();
        } else if (policy == OverflowPolicy.DISPLACE) {
            queue = new CircularIntBuffer(bufferSize, singleProducer);
        } else
            queue = new SingleConsumerArrayIntQueue(bufferSize);

        return new QueueIntChannel(queue, policy);
    }

    /**
     * Creates a new primitive {@code int} channel with the given mailbox size and {@link OverflowPolicy}, with other properties set to their default values.
     * Specifically, {@code singleProducer} will be set to {@code false}, while {@code singleConsumer} will be set to {@code true}.
     *
     * @param bufferSize if positive, the number of messages that the channel can hold in an internal buffer;
     *                   {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                   {@code -1} for a channel with an unbounded (infinite) buffer.
     * @param policy     the {@link OverflowPolicy} specifying how the channel (if bounded) will behave if its internal buffer overflows.
     * @return The newly created channel
     * @see #newIntChannel(int, co.paralleluniverse.strands.channels.Channels.OverflowPolicy, boolean, boolean)
     */
    public static IntChannel newIntChannel(int bufferSize, OverflowPolicy policy) {
        return newIntChannel(bufferSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    /**
     * Creates a new primitive {@code int} channel with the given mailbox size with other properties set to their default values.
     * Specifically, the {@link OverflowPolicy} will be set to {@link OverflowPolicy#BLOCK BLOCK},
     * {@code singleProducer} will be set to {@code false}, and {@code singleConsumer} will be set to {@code true}.
     *
     * @param bufferSize if positive, the number of messages that the channel can hold in an internal buffer;
     *                   {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                   {@code -1} for a channel with an unbounded (infinite) buffer.
     * @return The newly created channel
     * @see #newIntChannel(int, co.paralleluniverse.strands.channels.Channels.OverflowPolicy, boolean, boolean)
     */
    public static IntChannel newIntChannel(int bufferSize) {
        return newIntChannel(bufferSize, defaultPolicy);
    }

    ///
    /**
     * Creates a new primitive {@code long} channel with the given properties.
     * <p/>
     * Some combinations of properties are unsupported, and will throw an {@code IllegalArgumentException} if requested:
     *
     * <ul>
     * <li>multiple consumers</li>
     * <li>a transfer channel with any overflow policy other than {@link OverflowPolicy#BLOCK BLOCK}</li>
     * <li>An overflow policy of {@link OverflowPolicy#DISPLACE DISPLACE} with multiple consumers.</li>
     * </ul>
     * An unbounded channel ignores its overflow policy as it never overflows.
     *
     * @param bufferSize     if positive, the number of messages that the channel can hold in an internal buffer;
     *                       {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                       {@code -1} for a channel with an unbounded (infinite) buffer.
     * @param policy         the {@link OverflowPolicy} specifying how the channel (if bounded) will behave if its internal buffer overflows.
     * @param singleProducer whether the channel will be used by a single producer strand.
     * @param singleConsumer whether the channel will be used by a single consumer strand. Currently primitive channels only support a single
     *                       consumer, so this argument must be set to {@code false}.
     * @return The newly created channel
     */
    public static LongChannel newLongChannel(int bufferSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (!singleConsumer)
            throw new IllegalArgumentException("Primitive queue with multiple consumers is unsupported");

        final BasicSingleConsumerLongQueue queue;
        if (bufferSize < 0) {
            queue = new SingleConsumerLinkedArrayLongQueue();
        } else if (policy == OverflowPolicy.DISPLACE) {
            queue = new CircularLongBuffer(bufferSize, singleProducer);
        } else
            queue = new SingleConsumerArrayLongQueue(bufferSize);

        return new QueueLongChannel(queue, policy);
    }

    /**
     * Creates a new primitive {@code long} channel with the given mailbox size and {@link OverflowPolicy}, with other properties set to their default values.
     * Specifically, {@code singleProducer} will be set to {@code false}, while {@code singleConsumer} will be set to {@code true}.
     *
     * @param bufferSize if positive, the number of messages that the channel can hold in an internal buffer;
     *                   {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                   {@code -1} for a channel with an unbounded (infinite) buffer.
     * @param policy     the {@link OverflowPolicy} specifying how the channel (if bounded) will behave if its internal buffer overflows.
     * @return The newly created channel
     * @see #newLongChannel(int, co.paralleluniverse.strands.channels.Channels.OverflowPolicy, boolean, boolean)
     */
    public static LongChannel newLongChannel(int bufferSize, OverflowPolicy policy) {
        return newLongChannel(bufferSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    /**
     * Creates a new primitive {@code long} channel with the given mailbox size with other properties set to their default values.
     * Specifically, the {@link OverflowPolicy} will be set to {@link OverflowPolicy#BLOCK BLOCK},
     * {@code singleProducer} will be set to {@code false}, and {@code singleConsumer} will be set to {@code true}.
     *
     * @param bufferSize if positive, the number of messages that the channel can hold in an internal buffer;
     *                   {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                   {@code -1} for a channel with an unbounded (infinite) buffer.
     * @return The newly created channel
     * @see #newLongChannel(int, co.paralleluniverse.strands.channels.Channels.OverflowPolicy, boolean, boolean)
     */
    public static LongChannel newLongChannel(int bufferSize) {
        return newLongChannel(bufferSize, defaultPolicy);
    }

    ///
    /**
     * Creates a new primitive {@code float} channel with the given properties.
     * <p/>
     * Some combinations of properties are unsupported, and will throw an {@code IllegalArgumentException} if requested:
     *
     * <ul>
     * <li>multiple consumers</li>
     * <li>a transfer channel with any overflow policy other than {@link OverflowPolicy#BLOCK BLOCK}</li>
     * <li>An overflow policy of {@link OverflowPolicy#DISPLACE DISPLACE} with multiple consumers.</li>
     * </ul>
     * An unbounded channel ignores its overflow policy as it never overflows.
     *
     * @param bufferSize     if positive, the number of messages that the channel can hold in an internal buffer;
     *                       {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                       {@code -1} for a channel with an unbounded (infinite) buffer.
     * @param policy         the {@link OverflowPolicy} specifying how the channel (if bounded) will behave if its internal buffer overflows.
     * @param singleProducer whether the channel will be used by a single producer strand.
     * @param singleConsumer whether the channel will be used by a single consumer strand. Currently primitive channels only support a single
     *                       consumer, so this argument must be set to {@code false}.
     * @return The newly created channel
     */
    public static FloatChannel newFloatChannel(int bufferSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (!singleConsumer)
            throw new IllegalArgumentException("Primitive queue with multiple consumers is unsupported");

        final BasicSingleConsumerFloatQueue queue;
        if (bufferSize < 0) {
            queue = new SingleConsumerLinkedArrayFloatQueue();
        } else if (policy == OverflowPolicy.DISPLACE) {
            queue = new CircularFloatBuffer(bufferSize, singleProducer);
        } else
            queue = new SingleConsumerArrayFloatQueue(bufferSize);

        return new QueueFloatChannel(queue, policy);
    }

    /**
     * Creates a new primitive {@code float} channel with the given mailbox size and {@link OverflowPolicy}, with other properties set to their default values.
     * Specifically, {@code singleProducer} will be set to {@code false}, while {@code singleConsumer} will be set to {@code true}.
     *
     * @param bufferSize if positive, the number of messages that the channel can hold in an internal buffer;
     *                   {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                   {@code -1} for a channel with an unbounded (infinite) buffer.
     * @param policy     the {@link OverflowPolicy} specifying how the channel (if bounded) will behave if its internal buffer overflows.
     * @return The newly created channel
     * @see #newFloatChannel(int, co.paralleluniverse.strands.channels.Channels.OverflowPolicy, boolean, boolean)
     */
    public static FloatChannel newFloatChannel(int bufferSize, OverflowPolicy policy) {
        return newFloatChannel(bufferSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    /**
     * Creates a new primitive {@code float} channel with the given mailbox size with other properties set to their default values.
     * Specifically, the {@link OverflowPolicy} will be set to {@link OverflowPolicy#BLOCK BLOCK},
     * {@code singleProducer} will be set to {@code false}, and {@code singleConsumer} will be set to {@code true}.
     *
     * @param bufferSize if positive, the number of messages that the channel can hold in an internal buffer;
     *                   {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                   {@code -1} for a channel with an unbounded (infinite) buffer.
     * @return The newly created channel
     * @see #newFloatChannel(int, co.paralleluniverse.strands.channels.Channels.OverflowPolicy, boolean, boolean)
     */
    public static FloatChannel newFloatChannel(int bufferSize) {
        return newFloatChannel(bufferSize, defaultPolicy);
    }

    ///
    /**
     * Creates a new primitive {@code double} channel with the given properties.
     * <p/>
     * Some combinations of properties are unsupported, and will throw an {@code IllegalArgumentException} if requested:
     *
     * <ul>
     * <li>multiple consumers</li>
     * <li>a transfer channel with any overflow policy other than {@link OverflowPolicy#BLOCK BLOCK}</li>
     * <li>An overflow policy of {@link OverflowPolicy#DISPLACE DISPLACE} with multiple consumers.</li>
     * </ul>
     * An unbounded channel ignores its overflow policy as it never overflows.
     *
     * @param bufferSize     if positive, the number of messages that the channel can hold in an internal buffer;
     *                       {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                       {@code -1} for a channel with an unbounded (infinite) buffer.
     * @param policy         the {@link OverflowPolicy} specifying how the channel (if bounded) will behave if its internal buffer overflows.
     * @param singleProducer whether the channel will be used by a single producer strand.
     * @param singleConsumer whether the channel will be used by a single consumer strand. Currently primitive channels only support a single
     *                       consumer, so this argument must be set to {@code false}.
     * @return The newly created channel
     */
    public static DoubleChannel newDoubleChannel(int bufferSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (!singleConsumer)
            throw new IllegalArgumentException("Primitive queue with multiple consumers is unsupported");

        final BasicSingleConsumerDoubleQueue queue;
        if (bufferSize < 0) {
            queue = new SingleConsumerLinkedArrayDoubleQueue();
        } else if (policy == OverflowPolicy.DISPLACE) {
            queue = new CircularDoubleBuffer(bufferSize, singleProducer);
        } else
            queue = new SingleConsumerArrayDoubleQueue(bufferSize);

        return new QueueDoubleChannel(queue, policy);
    }

    /**
     * Creates a new primitive {@code double} channel with the given mailbox size and {@link OverflowPolicy}, with other properties set to their default values.
     * Specifically, {@code singleProducer} will be set to {@code false}, while {@code singleConsumer} will be set to {@code true}.
     *
     * @param bufferSize if positive, the number of messages that the channel can hold in an internal buffer;
     *                   {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                   {@code -1} for a channel with an unbounded (infinite) buffer.
     * @param policy     the {@link OverflowPolicy} specifying how the channel (if bounded) will behave if its internal buffer overflows.
     * @return The newly created channel
     * @see #newDoubleChannel(int, co.paralleluniverse.strands.channels.Channels.OverflowPolicy, boolean, boolean)
     */
    public static DoubleChannel newDoubleChannel(int bufferSize, OverflowPolicy policy) {
        return newDoubleChannel(bufferSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    /**
     * Creates a new primitive {@code double} channel with the given mailbox size with other properties set to their default values.
     * Specifically, the {@link OverflowPolicy} will be set to {@link OverflowPolicy#BLOCK BLOCK},
     * {@code singleProducer} will be set to {@code false}, and {@code singleConsumer} will be set to {@code true}.
     *
     * @param bufferSize if positive, the number of messages that the channel can hold in an internal buffer;
     *                   {@code 0} for a <i>transfer</i> channel, i.e. a channel with no internal buffer.
     *                   {@code -1} for a channel with an unbounded (infinite) buffer.
     * @return The newly created channel
     * @see #newDoubleChannel(int, co.paralleluniverse.strands.channels.Channels.OverflowPolicy, boolean, boolean)
     */
    public static DoubleChannel newDoubleChannel(int bufferSize) {
        return newDoubleChannel(bufferSize, defaultPolicy);
    }

    ///
    /**
     * Creates a {@link ReceivePort} that can be used to receive messages from a a <i>ticker channel</i>:
     * a channel of bounded capacity and the {@link OverflowPolicy#DISPLACE DISPLACE} overflow policy.
     * Each ticker consumer will yield monotonic messages, namely no message will be received more than once, and the messages will
     * be received in the order they're sent, but if the consumer is too slow, messages could be lost.
     *
     * @param <Message> the message type
     * @param channel   a channel of bounded capacity and the {@link OverflowPolicy#DISPLACE DISPLACE} overflow policy.
     * @return a new {@link ReceivePort} which provides a view to the supplied ticker channel.
     */
    public static <Message> ReceivePort<Message> newTickerConsumerFor(Channel<Message> channel) {
        return TickerChannelConsumer.newFor((QueueChannel<Message>) channel);
    }

    /**
     * Creates an {@link IntReceivePort} that can be used to receive messages from a a <i>ticker channel</i>:
     * a channel of bounded capacity and the {@link OverflowPolicy#DISPLACE DISPLACE} overflow policy.
     * Each ticker consumer will yield monotonic messages, namely no message will be received more than once, and the messages will
     * be received in the order they're sent, but if the consumer is too slow, messages could be lost.
     *
     * @param channel an {@code int} channel of bounded capacity and the {@link OverflowPolicy#DISPLACE DISPLACE} overflow policy.
     * @return a new {@link IntReceivePort} which provides a view to the supplied ticker channel.
     */
    public static IntReceivePort newTickerConsumerFor(IntChannel channel) {
        return TickerChannelConsumer.newFor((QueueIntChannel) channel);
    }

    /**
     * Creates a {@link LongReceivePort} that can be used to receive messages from a a <i>ticker channel</i>:
     * a channel of bounded capacity and the {@link OverflowPolicy#DISPLACE DISPLACE} overflow policy.
     * Each ticker consumer will yield monotonic messages, namely no message will be received more than once, and the messages will
     * be received in the order they're sent, but if the consumer is too slow, messages could be lost.
     *
     * @param channel a {@code long} channel of bounded capacity and the {@link OverflowPolicy#DISPLACE DISPLACE} overflow policy.
     * @return a new {@link LongReceivePort} which provides a view to the supplied ticker channel.
     */
    public static LongReceivePort newTickerConsumerFor(LongChannel channel) {
        return TickerChannelConsumer.newFor((QueueLongChannel) channel);
    }

    /**
     * Creates a {@link FloatReceivePort} that can be used to receive messages from a a <i>ticker channel</i>:
     * a channel of bounded capacity and the {@link OverflowPolicy#DISPLACE DISPLACE} overflow policy.
     * Each ticker consumer will yield monotonic messages, namely no message will be received more than once, and the messages will
     * be received in the order they're sent, but if the consumer is too slow, messages could be lost.
     *
     * @param channel a {@code float} channel of bounded capacity and the {@link OverflowPolicy#DISPLACE DISPLACE} overflow policy.
     * @return a new {@link FloatReceivePort} which provides a view to the supplied ticker channel.
     */
    public static FloatReceivePort newTickerConsumerFor(FloatChannel channel) {
        return TickerChannelConsumer.newFor((QueueFloatChannel) channel);
    }

    /**
     * Creates a {@link DoubleReceivePort} that can be used to receive messages from a a <i>ticker channel</i>:
     * a channel of bounded capacity and the {@link OverflowPolicy#DISPLACE DISPLACE} overflow policy.
     * Each ticker consumer will yield monotonic messages, namely no message will be received more than once, and the messages will
     * be received in the order they're sent, but if the consumer is too slow, messages could be lost.
     *
     * @param channel a {@code double} channel of bounded capacity and the {@link OverflowPolicy#DISPLACE DISPLACE} overflow policy.
     * @return a new {@link DoubleReceivePort} which provides a view to the supplied ticker channel.
     */
    public static DoubleReceivePort newTickerConsumerFor(DoubleChannel channel) {
        return TickerChannelConsumer.newFor((QueueDoubleChannel) channel);
    }

    ////////////////////
    public static <M> ReceivePort<M> group(ReceivePort<? extends M>... channels) {
        return new ReceivePortGroup<M>(channels);
    }

    public static <M> ReceivePort<M> filter(ReceivePort<M> channel, Predicate<M> pred) {
        return new FilteringReceivePort<M>(channel, pred);
    }

    public static <S, T> ReceivePort<T> map(ReceivePort<S> channel, Function<S, T> f) {
        return new MappingReceivePort<S, T>(channel, f);
    }

    public static <M> ReceivePort<M> zip(Function<Object[], M> f, ReceivePort<?>... cs) {
        return new ZippingReceivePort<M>(f, cs);
    }

    public static <M, S1, S2> ReceivePort<M> zip(ReceivePort<S1> c1, ReceivePort<S2> c2, final Function2<S1, S2, M> f) {
        return new ZippingReceivePort<M>(c1, c2) {
            @Override
            protected M transform(Object[] ms) {
                return f.apply((S1) ms[0], (S2) ms[1]);
            }
        };
    }

    public static <M, S1, S2, S3> ReceivePort<M> zip(ReceivePort<S1> c1, ReceivePort<S2> c2, ReceivePort<S3> c3, final Function3<S1, S2, S3, M> f) {
        return new ZippingReceivePort<M>(c1, c2, c3) {
            @Override
            protected M transform(Object[] ms) {
                return f.apply((S1) ms[0], (S2) ms[1], (S3) ms[2]);
            }
        };
    }

    public static <M, S1, S2, S3, S4> ReceivePort<M> zip(ReceivePort<S1> c1, ReceivePort<S2> c2, ReceivePort<S3> c3, ReceivePort<S4> c4,
            final Function4<S1, S2, S3, S4, M> f) {
        return new ZippingReceivePort<M>(c1, c2, c3, c4) {
            @Override
            protected M transform(Object[] ms) {
                return f.apply((S1) ms[0], (S2) ms[1], (S3) ms[2], (S4) ms[3]);
            }
        };
    }

    public static <M, S1, S2, S3, S4, S5> ReceivePort<M> zip(ReceivePort<S1> c1, ReceivePort<S2> c2, ReceivePort<S3> c3, ReceivePort<S4> c4, ReceivePort<S5> c5,
            final Function5<S1, S2, S3, S4, S5, M> f) {
        return new ZippingReceivePort<M>(c1, c2, c3, c4, c5) {
            @Override
            protected M transform(Object[] ms) {
                return f.apply((S1) ms[0], (S2) ms[1], (S3) ms[2], (S4) ms[3], (S5) ms[4]);
            }
        };
    }

    public static <M> SendPort<M> filter(SendPort<M> channel, Predicate<M> pred) {
        return new FilteringSendPort<M>(channel, pred);
    }

    public static <S, T> SendPort<S> map(SendPort<T> channel, Function<S, T> f) {
        return new MappingSendPort<S, T>(channel, f);
    }

    private Channels() {
    }
}
