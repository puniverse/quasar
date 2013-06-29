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
import co.paralleluniverse.strands.queues.SingleProducerCircularFloatBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class TickerFloatChannel extends TickerChannel<Float> implements FloatSendPort, FloatReceivePort {
    public static TickerFloatChannel create(Object owner, int size) {
        return new TickerFloatChannel(owner, size);
    }

    public static TickerFloatChannel create(int size) {
        return new TickerFloatChannel(size);
    }

    public TickerFloatChannel(Object owner, int size) {
        super(owner, new SingleProducerCircularFloatBuffer(size));
    }

    private TickerFloatChannel(int size) {
        super(new SingleProducerCircularFloatBuffer(size));
    }

    @Override
    public void send(float message) {
        if (isSendClosed())
            return;
        buffer().enq(message);
        signal();
    }

    @Override
    public boolean trySend(float message) {
        send(message);
        return true;
    }

    @Override
    public float receiveFloat() throws SuspendExecution, InterruptedException {
        return ((TickerChannelFloatConsumer) consumer).receiveFloat();
    }

    @Override
    public float receiveFloat(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException {
        return ((TickerChannelFloatConsumer) consumer).receiveFloat(timeout, unit);
    }

    @Override
    public TickerChannelFloatConsumer newConsumer() {
        return new TickerChannelFloatConsumer(this);
    }

    private SingleProducerCircularFloatBuffer buffer() {
        return (SingleProducerCircularFloatBuffer) buffer;
    }

    public static class TickerChannelFloatConsumer extends TickerChannelConsumer<Float> implements FloatReceivePort {
        public TickerChannelFloatConsumer(TickerFloatChannel channel) {
            super(channel);
        }

        @Override
        public float receiveFloat() throws SuspendExecution, InterruptedException {
            attemptReceive();
            return consumer().getFloatValue();
        }

        @Override
        public float receiveFloat(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException {
            attemptReceive(timeout, unit);
            return consumer().getFloatValue();
        }

        private SingleProducerCircularFloatBuffer.FloatConsumer consumer() {
            return (SingleProducerCircularFloatBuffer.FloatConsumer) consumer;
        }
    }
}
