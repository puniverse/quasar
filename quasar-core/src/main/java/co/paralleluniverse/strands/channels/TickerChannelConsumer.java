/*
 * Quasar: lightweight strands and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Condition;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.queues.CircularBuffer;
import co.paralleluniverse.strands.queues.CircularDoubleBuffer;
import co.paralleluniverse.strands.queues.CircularFloatBuffer;
import co.paralleluniverse.strands.queues.CircularIntBuffer;
import co.paralleluniverse.strands.queues.CircularLongBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link ReceivePort} which is a view of a {@link Channels#newTickerConsumerFor(co.paralleluniverse.strands.channels.Channel) <i>ticker channel</i>}.
 * Each ticker-consumer will yield monotonic messages, namely no message will be received more than once, and the messages will be
 * received in the order they're sent, but if the consumer is too slow, messages could be lost.
 *
 * @author pron
 */
public class TickerChannelConsumer<Message> implements ReceivePort<Message>, Selectable<Message> {
    static <Message> ReceivePort<Message> newFor(QueueChannel<Message> channel) {
        return new TickerChannelConsumer<>(channel);
    }

    static IntReceivePort newFor(QueueIntChannel channel) {
        return new TickerChannelIntConsumer(channel);
    }

    static LongReceivePort newFor(QueueLongChannel channel) {
        return new TickerChannelLongConsumer(channel);
    }

    static FloatReceivePort newFor(QueueFloatChannel channel) {
        return new TickerChannelFloatConsumer(channel);
    }

    static DoubleReceivePort newFor(QueueDoubleChannel channel) {
        return new TickerChannelDoubleConsumer(channel);
    }
    ////
    final QueueChannel<Message> channel;
    final CircularBuffer<Message>.Consumer consumer;
    private boolean receiveClosed;

    private TickerChannelConsumer(QueueChannel<Message> channel) {
        this.channel = channel;
        this.consumer = ((CircularBuffer<Message>) channel.queue).newConsumer();
    }

    void setReceiveClosed() {
        this.receiveClosed = true;
    }

    protected void checkClosed() throws EOFException {
        if (isClosed()) {
            if (channel.getCloseException() != null)
                throw new ProducerException(channel.getCloseException());
            throw EOFException.instance;
        }
    }

    public boolean hasMessage() {
        return consumer.hasNext();
    }
    
    void attemptReceive() throws EOFException, SuspendExecution, InterruptedException {
        checkClosed();
        final Condition sync = channel.sync;
        Object token = sync.register();
        try {
            for (int i = 0; !consumer.hasNext(); i++) {
                if (channel.isSendClosed()) {
                    setReceiveClosed();
                    checkClosed();
                }
                sync.await(i);
            }
            consumer.poll0();
        } finally {
            sync.unregister(token);
        }
    }

    void attemptReceive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
        checkClosed();
        final Condition sync = channel.sync;
        long left = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + left;
        Object token = sync.register();
        try {
            for (int i = 0; !consumer.hasNext(); i++) {
                if (channel.isSendClosed()) {
                    setReceiveClosed();
                    checkClosed();
                }
                sync.await(i, left, TimeUnit.NANOSECONDS);
                left = deadline - System.nanoTime();
                if (left <= 0)
                    throw new TimeoutException();
            }
            consumer.poll0();
        } finally {
            sync.unregister(token);
        }
    }

    public final long getLastIndexRead() {
        return consumer.lastIndexRead();
    }

    @Override
    public Message tryReceive() {
        if (!consumer.hasNext())
            return null;
        return (Message) consumer.poll();
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        try {
            attemptReceive();
            return (Message) consumer.getAndClearReadValue();
        } catch (EOFException e) {
            return null;
        }
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        try {
            attemptReceive(timeout, unit);
            return (Message) consumer.getAndClearReadValue();
        } catch (EOFException e) {
            return null;
        } catch (TimeoutException e) {
            return null;
        }
    }

    @Override
    public Message receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    @Override
    public void close() {
        setReceiveClosed();
    }

    @Override
    public boolean isClosed() {
        return receiveClosed;
    }

    @Override
    public Object register(SelectAction<Message> action) {
        if (((SelectActionImpl)action).isData())
            throw new UnsupportedOperationException("Send is not supported by TickerChannelConsumer");
        return channel.register(action);
    }

    @Override
    public boolean tryNow(Object token) {
        SelectActionImpl<Message> action = (SelectActionImpl<Message>) token;
        if (!action.lease())
            return false;
        boolean res;
        assert !action.isData();

        Message m = tryReceive();
        action.setItem(m);
        if (m == null)
            res = isClosed();
        else
            res = true;

        if (res)
            action.won();
        else
            action.returnLease();
        return res;
    }

    @Override
    public void unregister(Object token) {
        channel.unregister(token);
    }

    ////////////
    private static class TickerChannelIntConsumer extends TickerChannelConsumer<Integer> implements IntReceivePort {
        public TickerChannelIntConsumer(QueueIntChannel channel) {
            super(channel);
        }

        @Override
        public int receiveInt() throws SuspendExecution, InterruptedException, EOFException {
            attemptReceive();
            return consumer().getIntValue();
        }

        @Override
        public int receiveInt(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
            attemptReceive(timeout, unit);
            return consumer().getIntValue();
        }

        @Override
        public int receiveInt(Timeout timeout) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
            return receiveInt(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
        }

        private CircularIntBuffer.IntConsumer consumer() {
            return (CircularIntBuffer.IntConsumer) consumer;
        }
    }

    private static class TickerChannelLongConsumer extends TickerChannelConsumer<Long> implements LongReceivePort {
        public TickerChannelLongConsumer(QueueLongChannel channel) {
            super(channel);
        }

        @Override
        public long receiveLong() throws SuspendExecution, InterruptedException, EOFException {
            attemptReceive();
            return consumer().getLongValue();
        }

        @Override
        public long receiveLong(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
            attemptReceive(timeout, unit);
            return consumer().getLongValue();
        }

        @Override
        public long receiveLong(Timeout timeout) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
            return receiveLong(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
        }

        private CircularLongBuffer.LongConsumer consumer() {
            return (CircularLongBuffer.LongConsumer) consumer;
        }
    }

    public static class TickerChannelFloatConsumer extends TickerChannelConsumer<Float> implements FloatReceivePort {
        public TickerChannelFloatConsumer(QueueFloatChannel channel) {
            super(channel);
        }

        @Override
        public float receiveFloat() throws SuspendExecution, InterruptedException, EOFException {
            attemptReceive();
            return consumer().getFloatValue();
        }

        @Override
        public float receiveFloat(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
            attemptReceive(timeout, unit);
            return consumer().getFloatValue();
        }

        @Override
        public float receiveFloat(Timeout timeout) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
            return receiveFloat(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
        }

        private CircularFloatBuffer.FloatConsumer consumer() {
            return (CircularFloatBuffer.FloatConsumer) consumer;
        }
    }

    private static class TickerChannelDoubleConsumer extends TickerChannelConsumer<Double> implements DoubleReceivePort {
        public TickerChannelDoubleConsumer(QueueDoubleChannel channel) {
            super(channel);
        }

        @Override
        public double receiveDouble() throws SuspendExecution, InterruptedException, EOFException {
            attemptReceive();
            return consumer().getDoubleValue();
        }

        @Override
        public double receiveDouble(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
            attemptReceive(timeout, unit);
            return consumer().getDoubleValue();
        }

        @Override
        public double receiveDouble(Timeout timeout) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
            return receiveDouble(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
        }

        private CircularDoubleBuffer.DoubleConsumer consumer() {
            return (CircularDoubleBuffer.DoubleConsumer) consumer;
        }
    }
}
