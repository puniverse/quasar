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
package co.paralleluniverse.strands.channels.disruptor;

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.ReceivePort;
import com.lmax.disruptor.AbstractSequencer;
import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.Sequencer;
import com.lmax.disruptor.WaitStrategy;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class DisruptorReceiveChannel<Message> implements ReceivePort<Message> {
    private final SequenceBarrier barrier;
    private final RingBuffer<Message> buffer;
    private final Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
    private long availableSequence = -1L;
    private volatile boolean closed;

    public DisruptorReceiveChannel(RingBuffer<Message> buffer, Sequence... dependentSequences) {
        this.buffer = buffer;
        final Sequencer sequencer = getSequencer(buffer);
        final WaitStrategy waitStrategy = getWaitStrategy(sequencer);
        final Sequence cursor = getCursor(sequencer);

        if (!(waitStrategy instanceof StrandBlockingWaitStrategy))
            throw new IllegalArgumentException("Channel can only be created from RingBuffer with StrandBlockingWaitStrategy");
        this.barrier = new ProcessingSequenceBarrier(sequencer, waitStrategy, cursor, dependentSequences);
        barrier.clearAlert();
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        if (closed)
            return null;
        long nextSequence = sequence.get() + 1L;
        try {
            while (nextSequence > availableSequence)
                availableSequence = barrier.waitFor1(nextSequence);
            Message message = buffer.get(nextSequence);
            return message;
        } catch (AlertException ex) {
            return null;
        }
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (unit == null)
            return receive();
        if (timeout <= 0)
            return tryReceive();

        if (closed)
            return null;
        try {
            long nextSequence = sequence.get() + 1L;
            if (nextSequence > availableSequence) {
                final long start = System.nanoTime();
                long left = unit.toNanos(timeout);
                final long deadline = start + unit.toNanos(timeout);

                while (nextSequence > availableSequence)
                    availableSequence = barrier.waitFor1(nextSequence, left, TimeUnit.NANOSECONDS);
                if (nextSequence > availableSequence) {
                    left = deadline - System.nanoTime();
                    if (left <= 0)
                        return null;
                }
            }
            Message message = buffer.get(nextSequence);
            return message;
        } catch (TimeoutException | AlertException e) {
            return null;
        }
    }

    @Override
    public Message receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    @Override
    public Message tryReceive() {
        if (closed)
            return null;
        long nextSequence = sequence.get() + 1L;
        if (nextSequence > availableSequence)
            return null;
        return buffer.get(nextSequence);
    }

    private static Sequencer getSequencer(RingBuffer<?> buffer) {
        try {
            return (Sequencer) sequencerField.get(buffer);
        } catch (IllegalArgumentException ex) {
            throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            throw new Error(ex);
        }
    }

    private static Sequence getCursor(Sequencer sequencer) {
        try {
            return (Sequence) cursorField.get(sequencer);
        } catch (IllegalArgumentException ex) {
            throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            throw new Error(ex);
        }
    }

    private static WaitStrategy getWaitStrategy(Sequencer sequencer) {
        try {
            return (WaitStrategy) waitStrategyField.get(sequencer);
        } catch (IllegalArgumentException ex) {
            throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            throw new Error(ex);
        }
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
    private static final Field sequencerField;
    private static final Field cursorField;
    private static final Field waitStrategyField;

    static {
        try {
            sequencerField = RingBuffer.class.getSuperclass().getDeclaredField("sequencer");
            sequencerField.setAccessible(true);

            cursorField = AbstractSequencer.class.getDeclaredField("cursor");
            cursorField.setAccessible(true);

            waitStrategyField = AbstractSequencer.class.getDeclaredField("waitStrategy");
            waitStrategyField.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            throw new AssertionError(ex);
        } catch (SecurityException ex) {
            throw new Error(ex);
        }
    }
}
