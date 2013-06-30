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
import co.paralleluniverse.strands.queues.CircularLongBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class TickerLongChannel extends TickerChannel<Long> implements LongChannel {
    public TickerLongChannel(int size, boolean singleProducer) {
        super(new CircularLongBuffer(size, singleProducer));
    }

    @Override
    public void send(long message) {
        if (isSendClosed())
            return;
        buffer().enq(message);
        signal();
    }

    @Override
    public boolean trySend(long message) {
        send(message);
        return true;
    }

    @Override
    public long receiveLong() throws SuspendExecution, InterruptedException {
        return ((TickerChannelLongConsumer) consumer).receiveLong();
    }

    @Override
    public long receiveLong(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException {
        return ((TickerChannelLongConsumer) consumer).receiveLong(timeout, unit);
    }

    @Override
    public TickerChannelLongConsumer newConsumer() {
        return new TickerChannelLongConsumer(this);
    }

    public static TickerChannelLongConsumer newConsumer(LongChannel tickerChannel) {
        return ((TickerLongChannel) tickerChannel).newConsumer();
    }

    private CircularLongBuffer buffer() {
        return (CircularLongBuffer) buffer;
    }

    public static class TickerChannelLongConsumer extends TickerChannelConsumer<Long> implements LongReceivePort {
        public TickerChannelLongConsumer(TickerLongChannel channel) {
            super(channel);
        }

        @Override
        public long receiveLong() throws SuspendExecution, InterruptedException {
            attemptReceive();
            return consumer().getLongValue();
        }

        @Override
        public long receiveLong(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException {
            attemptReceive(timeout, unit);
            return consumer().getLongValue();
        }

        private CircularLongBuffer.LongConsumer consumer() {
            return (CircularLongBuffer.LongConsumer) consumer;
        }
    }
}
