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

import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.QueueChannel.OverflowPolicy;
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
    private static final OverflowPolicy defaultPolicy = OverflowPolicy.THROW;
    private static final boolean defaultSingleProducer = false;
    private static final boolean defaultSingleConsumer = true;

    public static <Message> Channel<Message> newChannel(int mailboxSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (mailboxSize == 0 && policy == OverflowPolicy.BLOCK)
            return new TransferChannel<Message>();

        if (policy == OverflowPolicy.DISPLACE && mailboxSize > 0)
            return new TickerObjectChannel<Message>(mailboxSize, singleProducer);

        if (!singleConsumer)
            throw new UnsupportedOperationException("Channel with given configuration is not supported for multiple consumers");
        return new QueueObjectChannel(null,
                mailboxSize > 0
                ? new SingleConsumerArrayObjectQueue<Message>(mailboxSize)
                : new SingleConsumerLinkedArrayObjectQueue<Message>(),
                policy);
    }

    public static <Message> Channel<Message> newChannel(int mailboxSize, OverflowPolicy policy) {
        return newChannel(mailboxSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    public static <Message> Channel<Message> newChannel(int mailboxSize) {
        return newChannel(mailboxSize, mailboxSize == 0 ? OverflowPolicy.BLOCK : defaultPolicy);
    }

    public static <Message> Channel<Message> newChannel(Strand owner, int mailboxSize, OverflowPolicy policy, boolean singleProducer) {
        if (mailboxSize == 0 && policy == OverflowPolicy.BLOCK)
            return new TransferChannel<Message>();

        if (policy == OverflowPolicy.DISPLACE && mailboxSize >= 0)
            return new TickerObjectChannel<Message>(mailboxSize, singleProducer);

        return new QueueObjectChannel(owner,
                mailboxSize > 0
                ? new SingleConsumerArrayObjectQueue<Message>(mailboxSize)
                : new SingleConsumerLinkedArrayObjectQueue<Message>(),
                policy);
    }

    public static <Message> Channel<Message> newChannel(Strand owner, int mailboxSize, OverflowPolicy policy) {
        return newChannel(owner, mailboxSize, defaultPolicy, defaultSingleProducer);
    }

    public static <Message> Channel<Message> newChannel(Strand owner, int mailboxSize) {
        return newChannel(owner, mailboxSize, defaultPolicy);
    }

    ///
    public static IntChannel newIntChannel(int mailboxSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (policy == OverflowPolicy.DISPLACE && mailboxSize >= 0)
            return new TickerIntChannel(mailboxSize, singleProducer);

        if (!singleConsumer)
            throw new UnsupportedOperationException("Channel with given configuration is not supported for multiple consumers");
        return new QueueIntChannel(null,
                mailboxSize > 0
                ? new SingleConsumerArrayIntQueue(mailboxSize)
                : new SingleConsumerLinkedArrayIntQueue(),
                policy);
    }

    public static IntChannel newIntChannel(int mailboxSize, OverflowPolicy policy) {
        return newIntChannel(mailboxSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    public static IntChannel newIntChannel(int mailboxSize) {
        return newIntChannel(mailboxSize, defaultPolicy);
    }

    public static IntChannel newIntChannel(Strand owner, int mailboxSize, OverflowPolicy policy, boolean singleProducer) {
        if (policy == OverflowPolicy.DISPLACE && mailboxSize >= 0)
            return new TickerIntChannel(mailboxSize, singleProducer);

        return new QueueIntChannel(owner,
                mailboxSize > 0
                ? new SingleConsumerArrayIntQueue(mailboxSize)
                : new SingleConsumerLinkedArrayIntQueue(),
                policy);
    }

    public static IntChannel newIntChannel(Strand owner, int mailboxSize, OverflowPolicy policy) {
        return newIntChannel(owner, mailboxSize, policy, defaultSingleProducer);
    }

    public static IntChannel newIntChannel(Strand owner, int mailboxSize) {
        return newIntChannel(owner, mailboxSize, defaultPolicy);
    }

    ///
    public static LongChannel newLongChannel(int mailboxSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (policy == OverflowPolicy.DISPLACE && mailboxSize >= 0)
            return new TickerLongChannel(mailboxSize, singleProducer);

        if (!singleConsumer)
            throw new UnsupportedOperationException("Channel with given configuration is not supported for multiple consumers");
        return new QueueLongChannel(null,
                mailboxSize > 0
                ? new SingleConsumerArrayLongQueue(mailboxSize)
                : new SingleConsumerLinkedArrayLongQueue(),
                policy);
    }

    public static LongChannel newLongChannel(int mailboxSize, OverflowPolicy policy) {
        return newLongChannel(mailboxSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    public static LongChannel newLongChannel(int mailboxSize) {
        return newLongChannel(mailboxSize, defaultPolicy);
    }

    public static LongChannel newLongChannel(Strand owner, int mailboxSize, OverflowPolicy policy, boolean singleProducer) {
        if (policy == OverflowPolicy.DISPLACE && mailboxSize >= 0)
            return new TickerLongChannel(mailboxSize, singleProducer);

        return new QueueLongChannel(owner,
                mailboxSize > 0
                ? new SingleConsumerArrayLongQueue(mailboxSize)
                : new SingleConsumerLinkedArrayLongQueue(),
                policy);
    }

    public static LongChannel newLongChannel(Strand owner, int mailboxSize, OverflowPolicy policy) {
        return newLongChannel(owner, mailboxSize, policy, defaultSingleProducer);
    }

    public static LongChannel newLongChannel(Strand owner, int mailboxSize) {
        return newLongChannel(owner, mailboxSize, defaultPolicy);
    }

    ///
    public static FloatChannel newFloatChannel(int mailboxSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (policy == OverflowPolicy.DISPLACE && mailboxSize >= 0)
            return new TickerFloatChannel(mailboxSize, singleProducer);

        if (!singleConsumer)
            throw new UnsupportedOperationException("Channel with given configuration is not supported for multiple consumers");
        return new QueueFloatChannel(null,
                mailboxSize > 0
                ? new SingleConsumerArrayFloatQueue(mailboxSize)
                : new SingleConsumerLinkedArrayFloatQueue(),
                policy);
    }

    public static FloatChannel newFloatChannel(int mailboxSize, OverflowPolicy policy) {
        return newFloatChannel(mailboxSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    public static FloatChannel newFloatChannel(int mailboxSize) {
        return newFloatChannel(mailboxSize, defaultPolicy);
    }

    public static FloatChannel newFloatChannel(Strand owner, int mailboxSize, OverflowPolicy policy, boolean singleProducer) {
        if (policy == OverflowPolicy.DISPLACE && mailboxSize >= 0)
            return new TickerFloatChannel(mailboxSize, singleProducer);

        return new QueueFloatChannel(owner,
                mailboxSize > 0
                ? new SingleConsumerArrayFloatQueue(mailboxSize)
                : new SingleConsumerLinkedArrayFloatQueue(),
                policy);
    }

    public static FloatChannel newFloatChannel(Strand owner, int mailboxSize, OverflowPolicy policy) {
        return newFloatChannel(owner, mailboxSize, policy, defaultSingleProducer);
    }

    public static FloatChannel newFloatChannel(Strand owner, int mailboxSize) {
        return newFloatChannel(owner, mailboxSize, defaultPolicy);
    }

    ///
    public static DoubleChannel newDoubleChannel(int mailboxSize, OverflowPolicy policy, boolean singleProducer, boolean singleConsumer) {
        if (policy == OverflowPolicy.DISPLACE && mailboxSize >= 0)
            return new TickerDoubleChannel(mailboxSize, singleProducer);

        if (!singleConsumer)
            throw new UnsupportedOperationException("Channel with given configuration is not supported for multiple consumers");
        return new QueueDoubleChannel(null,
                mailboxSize > 0
                ? new SingleConsumerArrayDoubleQueue(mailboxSize)
                : new SingleConsumerLinkedArrayDoubleQueue(),
                policy);
    }

    public static DoubleChannel newDoubleChannel(int mailboxSize, OverflowPolicy policy) {
        return newDoubleChannel(mailboxSize, policy, defaultSingleProducer, defaultSingleConsumer);
    }

    public static DoubleChannel newDoubleChannel(int mailboxSize) {
        return newDoubleChannel(mailboxSize, defaultPolicy);
    }

    public static DoubleChannel newDoubleChannel(Strand owner, int mailboxSize, OverflowPolicy policy, boolean singleProducer) {
        if (policy == OverflowPolicy.DISPLACE && mailboxSize >= 0)
            return new TickerDoubleChannel(mailboxSize, singleProducer);

        return new QueueDoubleChannel(owner,
                mailboxSize > 0
                ? new SingleConsumerArrayDoubleQueue(mailboxSize)
                : new SingleConsumerLinkedArrayDoubleQueue(),
                policy);
    }

    public static DoubleChannel newDoubleChannel(Strand owner, int mailboxSize, OverflowPolicy policy) {
        return newDoubleChannel(owner, mailboxSize, policy, defaultSingleProducer);
    }

    public static DoubleChannel newDoubleChannel(Strand owner, int mailboxSize) {
        return newDoubleChannel(owner, mailboxSize, defaultPolicy);
    }

    private Channels() {
    }
}
