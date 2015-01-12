/*
 * Quasar: lightweight strands and actors for the JVM.
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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.common.util.DelegatingEquals;
import co.paralleluniverse.common.util.Function2;
import co.paralleluniverse.common.util.Function3;
import co.paralleluniverse.common.util.Function4;
import co.paralleluniverse.common.util.Function5;
import co.paralleluniverse.fibers.DefaultFiberFactory;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.StrandFactory;
import co.paralleluniverse.strands.SuspendableAction1;
import co.paralleluniverse.strands.SuspendableAction2;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.Timeout;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private static final FiberFactory defaultFiberFactory = DefaultFiberFactory.instance();

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
        } else if (bufferSize == 1 && policy != OverflowPolicy.DISPLACE) // for now we'll use CircularObjectBuffer for displace channels of size 1
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

    /**
     * Tests whether a given channel is a <i>ticker channel</i>, namely a channel with a bounded buffer 
     * and an {@link OverflowPolicy overflow policy} of {@code DISPLACE}.
     * A ticker channel can be passed to one of the {@link #newTickerConsumerFor(Channel) newTickerConsumerFor} methods.
     */
    public static boolean isTickerChannel(ReceivePort<?> channel) {
        return channel instanceof QueueChannel 
                && ((QueueChannel)channel).overflowPolicy == OverflowPolicy.DISPLACE && ((QueueChannel)channel).capacity() > 0;
    }
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
    /**
     * Spawns a fiber that transforms values read from the {@code in} channel and writes values to the {@code out} channel.
     * <p>
     * @param fiberFactory will be used to create the fiber
     * @param in           the input channel
     * @param out          the output channel
     * @param transformer  the transforming operation
     */
    public static <S, T> void fiberTransform(FiberFactory fiberFactory, final ReceivePort<S> in, final SendPort<T> out, final SuspendableAction2<? extends ReceivePort<? super S>, ? extends SendPort<? extends T>> transformer) {
        fiberFactory.newFiber(new SuspendableCallable<Void>() {

            @Override
            public Void run() throws SuspendExecution, InterruptedException {
                try {
                    ((SuspendableAction2) transformer).call(in, out);
                    out.close();
                } catch (ProducerException e) {
                    out.close(e.getCause());
                } catch (SuspendExecution | InterruptedException t) {
                    out.close(t);
                }
                return null;
            }
        }).start();
    }

    /**
     * Spawns a fiber that transforms values read from the {@code in} channel and writes values to the {@code out} channel.
     * <p>
     * When the transformation terminates. the output channel is automatically closed. If the transformation terminates abnormally
     * (throws an exception), the output channel is {@link SendPort#close(Throwable) closed with that exception}.
     *
     * @param in          the input channel
     * @param out         the output channel
     * @param transformer the transforming operation
     */
    public static <S, T> void fiberTransform(final ReceivePort<S> in, final SendPort<T> out, final SuspendableAction2<? extends ReceivePort<? super S>, ? extends SendPort<? extends T>> transformer) {
        fiberTransform(defaultFiberFactory, in, out, transformer);
    }

    /**
     * Returns a {@link FixedTapSendPort} that will always forward to a single {@link SendPort}.
     *
     * @param target        The tapped {@link SendPort}.
     * @param strandFactory The {@link StrandFactory} that will build send strands when the {@link SendPort} would block.
     * @param forwardTo     The additional {@link SendPort} that will receive messages.
     * @return a {@link FixedTapSendPort} that will always forward to a single {@code forwardTo}.
     */
    public static <M> SendPort<M> fixedSendTap(final SendPort<M> target, final SendPort<? super M> forwardTo, final StrandFactory strandFactory) {
        return new FixedTapSendPort<>(target, forwardTo, strandFactory);
    }

    /**
     * Returns a {@link FixedTapSendPort} that will always forward to a single {@link SendPort}. {@link DefaultFiberFactory} will build
     * send strands when the {@link SendPort} would block.
     *
     * @param target        The tapped {@link SendPort}.
     * @param forwardTo     The additional {@link SendPort} that will receive messages.
     * @return a {@link FixedTapSendPort} that will always forward to a single {@code forwardTo}.
     */
    public static <M> SendPort<M> fixedSendTap(final SendPort<M> target, final SendPort<? super M> forwardTo) {
        return new FixedTapSendPort<>(target, forwardTo);
    }

    /**
     * Returns a {@link FixedTapReceivePort} that will always forward to a single {@link SendPort}.
     *
     * @param target        The tapped {@link ReceivePort}.
     * @param strandFactory The {@link StrandFactory} that will build send strands when the {@link SendPort} would block.
     * @param forwardTo     The additional {@link SendPort} that will receive messages.
     * @return a {@link FixedTapReceivePort} that will always forward to a single {@code forwardTo}.
     */
    public static <M> ReceivePort<M> fixedReceiveTap(final ReceivePort<M> target, final SendPort<? super M> forwardTo, final StrandFactory strandFactory) {
        return new FixedTapReceivePort<>(target, forwardTo, strandFactory);
    }

    /**
     * Returns a {@link FixedTapReceivePort} that will always forward to a single {@link SendPort}. {@link DefaultFiberFactory} will build
     * send strands when the {@link SendPort} would block.
     *
     * @param target        The tapped {@link ReceivePort}.
     * @param forwardTo     The additional {@link SendPort} that will receive messages.
     * @return a {@link FixedTapReceivePort} that will always forward to a single {@code forwardTo}.
     */
    public static <M> ReceivePort<M> fixedReceiveTap(final ReceivePort<M> target, final SendPort<? super M> forwardTo) {
        return new FixedTapReceivePort<>(target, forwardTo);
    }

    /**
     * Returns a {@link ReceivePort} that receives messages from a set of channels. Messages from all given channels are funneled into
     * the returned channel.
     *
     * @param <M>
     * @param channels
     * @return a {@link ReceivePort} that receives messages from {@code channels}.
     */
    public static <M> ReceivePort<M> group(ReceivePort<? extends M>... channels) {
        return new ReceivePortGroup<>(channels);
    }

    /**
     * Returns a {@link ReceivePort} that receives messages from a set of channels. Messages from all given channels are funneled into
     * the returned channel.
     *
     * @param <M>
     * @param channels
     * @return a {@link ReceivePort} that receives messages from {@code channels}.
     */
    public static <M> ReceivePort<M> group(Collection<? extends ReceivePort<? extends M>> channels) {
        return new ReceivePortGroup<>(channels);
    }

    /**
     * Returns a {@link ReceivePort} that filters messages that satisfy a predicate from a given channel.
     * All messages (even those not satisfying the predicate) will be consumed from the original channel; those that don't satisfy the predicate will be silently discarded.
     * <p/>
     * The returned {@code ReceivePort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param <M>     the message type.
     * @param channel The channel to filter
     * @param pred    the filtering predicate
     * @return A {@link ReceivePort} that will receive all those messages from the original channel which satisfy the predicate (i.e. the predicate returns {@code true}).
     */
    public static <M> ReceivePort<M> filter(ReceivePort<M> channel, Predicate<M> pred) {
        return new FilteringReceivePort<>(channel, pred);
    }

    /**
     * Returns a {@link ReceivePort} that receives messages that are transformed by a given mapping function from a given channel.
     * <p>
     * The returned {@code ReceivePort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param <S>     the message type of the source (given) channel.
     * @param <T>     the message type of the target (returned) channel.
     * @param channel the channel to transform
     * @param f       the mapping function
     * @return a {@link ReceivePort} that returns messages that are the result of applying the mapping function to the messages received on the given channel.
     */
    public static <S, T> ReceivePort<T> map(ReceivePort<S> channel, Function<S, T> f) {
        return new MappingReceivePort<>(channel, f);
    }

    /**
     * Returns a {@link ReceivePort} that maps exceptions thrown by the underlying channel
     * (by channel transformations, or as a result of {@link SendPort#close(Throwable)} )
     * into messages.
     * <p>
     * The returned {@code ReceivePort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param <T>     the message type of the target (returned) channel.
     * @param channel the channel to transform
     * @param f       the exception mapping function
     */
    public static <T> ReceivePort<T> mapErrors(ReceivePort<T> channel, Function<Exception, T> f) {
        return new ErrorMappingReceivePort<>(channel, f);
    }

    /**
     * Returns a {@link ReceivePort} that receives messages that are transformed by a given flat-mapping function from a given channel.
     * Unlike {@link #map(ReceivePort, Function) map}, the mapping function does not returns a single output message for every input message, but
     * a new {@code ReceivePort}. All the returned ports are concatenated into a single {@code ReceivePort} that receives the messages received by all
     * the ports in order.
     * <p>
     * To return a single value the mapping function can make use of {@link #singletonReceivePort(Object) singletonReceivePort}. To return a collection,
     * it can make use of {@link #toReceivePort(Iterable) toReceivePort(Iterable)}. To emit no values, the function can return {@link #emptyReceivePort()}
     * or {@code null}.
     * <p>
     * The returned {@code ReceivePort} can only be safely used by a single receiver strand.
     * <p>
     * The returned {@code ReceivePort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param <S>     the message type of the source (given) channel.
     * @param <T>     the message type of the target (returned) channel.
     * @param channel the channel to transform
     * @param f       the mapping function
     * @return a {@link ReceivePort} that returns messages that are the result of applying the mapping function to the messages received on the given channel.
     */
    public static <S, T> ReceivePort<T> flatMap(ReceivePort<S> channel, Function<S, ReceivePort<T>> f) {
        return new FlatMappingReceivePort<>(channel, f);
    }

    /**
     * Performs the given action on each message received by the given channel.
     * This method returns only after all messages have been consumed and the channel has been closed.
     *
     * @param channel the channel
     * @param action  the actions
     */
    public static <T> void forEach(ReceivePort<T> channel, SuspendableAction1<T> action) throws SuspendExecution, InterruptedException {
        T m;
        while ((m = channel.receive()) != null) {
            action.call(m);
        }
    }

    /**
     * Returns a {@link ReceivePort} that combines each vector of messages from a list of channels into a single combined message.
     *
     * @param <M> The type of the combined message
     * @param f   The combining function
     * @param cs  A vector of channels
     * @return A zipping {@link ReceivePort}
     */
    public static <M> ReceivePort<M> zip(List<? extends ReceivePort<?>> cs, Function<Object[], M> f) {
        return new ZippingReceivePort<>(f, cs);
    }

    /**
     * Returns a {@link ReceivePort} that combines each vector of messages from a vector of channels into a single combined message.
     *
     * @param <M> The type of the combined message
     * @param f   The combining function
     * @return A zipping {@link ReceivePort}
     */
    public static <M, S1, S2> ReceivePort<M> zip(ReceivePort<S1> c1, ReceivePort<S2> c2, final Function2<S1, S2, M> f) {
        return new ZippingReceivePort<M>(c1, c2) {
            @Override
            protected M transform(Object[] ms) {
                return f.apply((S1) ms[0], (S2) ms[1]);
            }
        };
    }

    /**
     * Returns a {@link ReceivePort} that combines each vector of messages from a vector of channels into a single combined message.
     *
     * @param <M> The type of the combined message
     * @param f   The combining function
     * @return A zipping {@link ReceivePort}
     */
    public static <M, S1, S2, S3> ReceivePort<M> zip(ReceivePort<S1> c1, ReceivePort<S2> c2, ReceivePort<S3> c3, final Function3<S1, S2, S3, M> f) {
        return new ZippingReceivePort<M>(c1, c2, c3) {
            @Override
            protected M transform(Object[] ms) {
                return f.apply((S1) ms[0], (S2) ms[1], (S3) ms[2]);
            }
        };
    }

    /**
     * Returns a {@link ReceivePort} that combines each vector of messages from a vector of channels into a single combined message.
     *
     * @param <M> The type of the combined message
     * @param f   The combining function
     * @return A zipping {@link ReceivePort}
     */
    public static <M, S1, S2, S3, S4> ReceivePort<M> zip(ReceivePort<S1> c1, ReceivePort<S2> c2, ReceivePort<S3> c3, ReceivePort<S4> c4,
            final Function4<S1, S2, S3, S4, M> f) {
        return new ZippingReceivePort<M>(c1, c2, c3, c4) {
            @Override
            protected M transform(Object[] ms) {
                return f.apply((S1) ms[0], (S2) ms[1], (S3) ms[2], (S4) ms[3]);
            }
        };
    }

    /**
     * Returns a {@link ReceivePort} that combines each vector of messages from a vector of channels into a single combined message.
     *
     * @param <M> The type of the combined message
     * @param f   The combining function
     * @return A zipping {@link ReceivePort}
     */
    public static <M, S1, S2, S3, S4, S5> ReceivePort<M> zip(ReceivePort<S1> c1, ReceivePort<S2> c2, ReceivePort<S3> c3, ReceivePort<S4> c4, ReceivePort<S5> c5,
            final Function5<S1, S2, S3, S4, S5, M> f) {
        return new ZippingReceivePort<M>(c1, c2, c3, c4, c5) {
            @Override
            protected M transform(Object[] ms) {
                return f.apply((S1) ms[0], (S2) ms[1], (S3) ms[2], (S4) ms[3], (S5) ms[4]);
            }
        };
    }

    /**
     * Returns a {@link TransformingReceivePort} wrapping the given channel, which may be used for functional
     * transformations.
     */
    public static <M> TransformingReceivePort<M> transform(ReceivePort<M> channel) {
        return new TransformingReceivePort<>(channel);
    }

    /**
     * Returns a {@link SendPort} that filters messages that satisfy a predicate before sending to a given channel.
     * Messages that don't satisfy the predicate will be silently discarded when sent.
     * <p/>
     * The returned {@code SendPort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param <M>     the message type.
     * @param channel The channel to filter
     * @param pred    the filtering predicate
     * @return A {@link SendPort} that will send only those messages which satisfy the predicate (i.e. the predicate returns {@code true}) to the given channel.
     */
    public static <M> SendPort<M> filterSend(SendPort<M> channel, Predicate<M> pred) {
        return new FilteringSendPort<>(channel, pred);
    }

    /**
     * Returns a {@link SendPort} that transforms messages by applying a given mapping function before sending them to a given channel.
     * <p/>
     * The returned {@code SendPort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param <S>     the message type of the source (returned) channel.
     * @param <T>     the message type of the target (given) channel.
     * @param channel the channel to transform
     * @param f       the mapping function
     * @return a {@link SendPort} that passes messages to the given channel after transforming them by applying the mapping function.
     */
    public static <S, T> SendPort<S> mapSend(SendPort<T> channel, Function<S, T> f) {
        return new MappingSendPort<>(channel, f);
    }

    /**
     * Returns a {@link SendPort} that sends messages that are transformed by a given flat-mapping function into a given channel.
     * Unlike {@link #mapSend(SendPort, Function) map}, the mapping function does not returns a single output message for every input message, but
     * a new {@code ReceivePort}. All the returned ports are concatenated and sent to the channel.
     * <p/>
     * To return a single value the mapping function can make use of {@link #singletonReceivePort(Object) singletonReceivePort}. To return a collection,
     * it can make use of {@link #toReceivePort(Iterable) toReceivePort(Iterable)}. To emit no values, the function can return {@link #emptyReceivePort()}
     * or {@code null}.
     * <p/>
     * If multiple producers send messages into the channel, the messages from the {@code ReceivePort}s returned by the mapping function
     * may be interleaved with other messages.
     * <p/>
     * The returned {@code SendPort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param <S>     the message type of the source (given) channel.
     * @param <T>     the message type of the target (returned) channel.
     * @param pipe    an intermediate channel used in the flat-mapping operation. Messages are first sent to this channel before being transformed.
     * @param channel the channel to transform
     * @param f       the mapping function
     * @return a {@link ReceivePort} that returns messages that are the result of applying the mapping function to the messages received on the given channel.
     */
    public static <S, T> SendPort<S> flatMapSend(FiberFactory fiberFactory, Channel<S> pipe, SendPort<T> channel, final Function<S, ReceivePort<T>> f) {
        fiberTransform(fiberFactory, pipe, channel, new SuspendableAction2<ReceivePort<S>, SendPort<T>>() {

            @Override
            public void call(ReceivePort<S> in, SendPort<T> out) throws SuspendExecution, InterruptedException {
                S x;
                while ((x = in.receive()) != null) {
                    ReceivePort<T> xp = f.apply(x);
                    if (xp != null) {
                        T y;
                        while ((y = xp.receive()) != null)
                            out.send(y);
                    }
                }
            }
        });
        return new PipeChannel<>(pipe, channel);
    }

    public static <S, T> SendPort<S> flatMapSend(Channel<S> pipe, SendPort<T> channel, final Function<S, ReceivePort<T>> f) {
        return flatMapSend(defaultFiberFactory, pipe, channel, f);
    }

    /**
     * Returns a {@link TransformingSendPort} wrapping the given channel, which may be used for functional
     * transformations.
     */
    public static <M> TransformingSendPort<M> transformSend(SendPort<M> channel) {
        return new TransformingSendPort<M>(channel);
    }

    /**
     * Returns an empty {@link ReceivePort}. The port is closed and receives no messages;
     */
    public static <T> ReceivePort<T> emptyReceivePort() {
        return (ReceivePort<T>) EMPTY_RECEIVE_PORT;
    }

    /**
     * Returns a newly created {@link ReceivePort} that receives a single message: the object given to the function.
     * <p>
     * @param <T>
     * @param object the single object that will be returned by the {@code ReceivePort}.
     */
    public static <T> ReceivePort<T> singletonReceivePort(final T object) {
        if (object == null)
            return null;
        return new ReceivePort<T>() {
            private boolean closed;

            @Override
            public T receive() {
                return tryReceive();
            }

            @Override
            public T receive(long timeout, TimeUnit unit) {
                return tryReceive();
            }

            @Override
            public T receive(Timeout timeout) {
                return tryReceive();
            }

            @Override
            public T tryReceive() {
                if (closed)
                    return null;
                this.closed = true;
                return object;
            }

            @Override
            public void close() {
                this.closed = true;
            }

            @Override
            public boolean isClosed() {
                return closed;
            }
        };
    }

    /**
     * Returns a newly created {@link ReceivePort} that receives all the elements iterated by the iterator.
     * <p>
     * @param <T>
     * @param iterator the iterator to transform into a {@code ReceivePort}.
     */
    public static <T> ReceivePort<T> toReceivePort(final Iterator<T> iterator) {
        if (iterator == null)
            return null;
        return new ReceivePort<T>() {
            private Iterator<T> it = iterator;

            @Override
            public T receive() {
                return tryReceive();
            }

            @Override
            public T receive(long timeout, TimeUnit unit) {
                return tryReceive();
            }

            @Override
            public T receive(Timeout timeout) {
                return tryReceive();
            }

            @Override
            public T tryReceive() {
                return !isClosed() ? it.next() : null;
            }

            @Override
            public void close() {
                this.it = null;
            }

            @Override
            public boolean isClosed() {
                return it == null || !it.hasNext();
            }
        };
    }

    /**
     * Returns a newly created {@link ReceivePort} that receives all the elements iterated by the iterable.
     * <p>
     * @param <T>
     * @param iterable the iterable to transform into a {@code ReceivePort}.
     */
    public static <T> ReceivePort<T> toReceivePort(final Iterable<T> iterable) {
        if (iterable == null)
            return null;
        return toReceivePort(iterable.iterator());
    }

    private Channels() {
    }

    private static final ReceivePort EMPTY_RECEIVE_PORT = new ReceivePort() {

        @Override
        public Object receive() {
            return null;
        }

        @Override
        public Object receive(long timeout, TimeUnit unit) {
            return null;
        }

        @Override
        public Object receive(Timeout timeout) {
            return null;
        }

        @Override
        public Object tryReceive() {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isClosed() {
            return true;
        }
    };
    
    // Package-access utilities
    
    static boolean delegatingEquals(final Object target, final Object obj) {
        if (obj instanceof DelegatingEquals)
            return obj.equals(target);
        else
            return target.equals(obj);
    }
    
    static String delegatingToString(final Object self, final Object target) {
        if (self != null)
            return self.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(self)) + "{" + target + "}";
        else
            return null;
    }
}
