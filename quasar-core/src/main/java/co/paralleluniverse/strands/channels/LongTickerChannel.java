/*
 * Quasar: lightweight threads and actors for the JVM.
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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.queues.SingleProducerCircularLongBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class LongTickerChannel extends TickerChannel<Long> {
    public static LongTickerChannel create(Object owner, int size) {
        return new LongTickerChannel(owner, size);
    }

    public static LongTickerChannel create(int size) {
        return new LongTickerChannel(size);
    }

    public LongTickerChannel(Object owner, int size) {
        super(owner, new SingleProducerCircularLongBuffer(size));
    }

    private LongTickerChannel(int size) {
        super(new SingleProducerCircularLongBuffer(size));
    }

    public void send(long message) {
        if (isSendClosed())
            return;
        buffer().enq(message);
        signal();
    }

    @Override
    public TickerChannelLongConsumer newConsumer() {
        return new TickerChannelLongConsumer(this);
    }

    private SingleProducerCircularLongBuffer buffer() {
        return (SingleProducerCircularLongBuffer) buffer;
    }

    public static class TickerChannelLongConsumer extends TickerChannelConsumer<Long> {
        public TickerChannelLongConsumer(LongTickerChannel channel) {
            super(channel);
        }

        public long receiveLong() throws SuspendExecution, InterruptedException {
            attemptReceive();
            return consumer().getLongValue();
        }

        public long receiveInt(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException {
            attemptReceive(timeout, unit);
            return consumer().getLongValue();
        }

        private SingleProducerCircularLongBuffer.LongConsumer consumer() {
            return (SingleProducerCircularLongBuffer.LongConsumer) consumer;
        }
    }
}
