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

/**
 *
 * @author pron
 */
public final class Channels {
    public enum OverflowPolicy {
        THROW, DROP, BLOCK, BACKOFF, DISPLACE
    }
    private static final OverflowPolicy defaultPolicy = OverflowPolicy.THROW;
    private static final boolean defaultSingleProducer = false;
    private static final boolean defaultSingleConsumer = true;

    public static <Message> Channel<Message> newChannel(int mailboxSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (mailboxSize == 0) {
            if (policy != OverflowPolicy.BLOCK)
                throw new IllegalArgumentException("Cannot use policy " + policy + " for channel with size 0 (only BLOCK supported");
            return new SelectableTransferChannel<Message>();
        }

        final BasicQueue<Message> queue;
        if (mailboxSize < 0) {
            if (!singleConsumer)
                throw new UnsupportedOperationException("Unbounded queue with multiple consumers is unsupported");
            queue = new SingleConsumerLinkedArrayObjectQueue<Message>();
        } else if (mailboxSize == 1)
            queue = new BoxQueue<Message>(policy == OverflowPolicy.DISPLACE, singleConsumer);
        else if (policy == OverflowPolicy.DISPLACE) {
            if (!singleConsumer)
                throw new UnsupportedOperationException("Channel with DISPLACE policy configuration is not supported for multiple consumers");
            queue = new CircularObjectBuffer<Message>(mailboxSize, singleProducer);
        } else if (singleConsumer)
            queue = new SingleConsumerArrayObjectQueue<Message>(mailboxSize);
        else
            queue = new ArrayQueue<Message>(mailboxSize);


        return new QueueObjectChannel(queue, policy, singleConsumer);
    }

    public static <Message> Channel<Message> newChannel(int mailboxSize, OverflowPolicy policy) {
        return newChannel(mailboxSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    public static <Message> Channel<Message> newChannel(int mailboxSize) {
        return newChannel(mailboxSize, mailboxSize == 0 ? OverflowPolicy.BLOCK : defaultPolicy);
    }

    ///
    public static IntChannel newIntChannel(int mailboxSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (!singleConsumer)
            throw new UnsupportedOperationException("Primitive queue with multiple consumers is unsupported");

        final BasicSingleConsumerIntQueue queue;
        if (mailboxSize < 0) {
            queue = new SingleConsumerLinkedArrayIntQueue();
        } else if (policy == OverflowPolicy.DISPLACE) {
            queue = new CircularIntBuffer(mailboxSize, singleProducer);
        } else
            queue = new SingleConsumerArrayIntQueue(mailboxSize);

        return new QueueIntChannel(queue, policy);
    }

    public static IntChannel newIntChannel(int mailboxSize, OverflowPolicy policy) {
        return newIntChannel(mailboxSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    public static IntChannel newIntChannel(int mailboxSize) {
        return newIntChannel(mailboxSize, defaultPolicy);
    }

    ///
    public static LongChannel newLongChannel(int mailboxSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (!singleConsumer)
            throw new UnsupportedOperationException("Primitive queue with multiple consumers is unsupported");

        final BasicSingleConsumerLongQueue queue;
        if (mailboxSize < 0) {
            queue = new SingleConsumerLinkedArrayLongQueue();
        } else if (policy == OverflowPolicy.DISPLACE) {
            queue = new CircularLongBuffer(mailboxSize, singleProducer);
        } else
            queue = new SingleConsumerArrayLongQueue(mailboxSize);

        return new QueueLongChannel(queue, policy);
    }

    public static LongChannel newLongChannel(int mailboxSize, OverflowPolicy policy) {
        return newLongChannel(mailboxSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    public static LongChannel newLongChannel(int mailboxSize) {
        return newLongChannel(mailboxSize, defaultPolicy);
    }

    ///
    public static FloatChannel newFloatChannel(int mailboxSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (!singleConsumer)
            throw new UnsupportedOperationException("Primitive queue with multiple consumers is unsupported");

        final BasicSingleConsumerFloatQueue queue;
        if (mailboxSize < 0) {
            queue = new SingleConsumerLinkedArrayFloatQueue();
        } else if (policy == OverflowPolicy.DISPLACE) {
            queue = new CircularFloatBuffer(mailboxSize, singleProducer);
        } else
            queue = new SingleConsumerArrayFloatQueue(mailboxSize);

        return new QueueFloatChannel(queue, policy);
    }

    public static FloatChannel newFloatChannel(int mailboxSize, OverflowPolicy policy) {
        return newFloatChannel(mailboxSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    public static FloatChannel newFloatChannel(int mailboxSize) {
        return newFloatChannel(mailboxSize, defaultPolicy);
    }

    ///
    public static DoubleChannel newDoubleChannel(int mailboxSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (!singleConsumer)
            throw new UnsupportedOperationException("Primitive queue with multiple consumers is unsupported");

        final BasicSingleConsumerDoubleQueue queue;
        if (mailboxSize < 0) {
            queue = new SingleConsumerLinkedArrayDoubleQueue();
        } else if (policy == OverflowPolicy.DISPLACE) {
            queue = new CircularDoubleBuffer(mailboxSize, singleProducer);
        } else
            queue = new SingleConsumerArrayDoubleQueue(mailboxSize);

        return new QueueDoubleChannel(queue, policy);
    }

    public static DoubleChannel newDoubleChannel(int mailboxSize, OverflowPolicy policy) {
        return newDoubleChannel(mailboxSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    public static DoubleChannel newDoubleChannel(int mailboxSize) {
        return newDoubleChannel(mailboxSize, defaultPolicy);
    }

    ///
    public static <Message> ReceivePort<Message> newTickerConsumerFor(Channel<Message> channel) {
        return TickerChannelConsumer.newFor((QueueChannel<Message>) channel);
    }

    public static IntReceivePort newTickerConsumerFor(IntChannel channel) {
        return TickerChannelConsumer.newFor((QueueIntChannel) channel);
    }

    public static LongReceivePort newTickerConsumerFor(QueueLongChannel channel) {
        return TickerChannelConsumer.newFor((QueueLongChannel) channel);
    }

    public static FloatReceivePort newTickerConsumerFor(QueueFloatChannel channel) {
        return TickerChannelConsumer.newFor((QueueFloatChannel) channel);
    }

    public static DoubleReceivePort newTickerConsumerFor(QueueDoubleChannel channel) {
        return TickerChannelConsumer.newFor((QueueDoubleChannel) channel);
    }

    private Channels() {
    }
}
