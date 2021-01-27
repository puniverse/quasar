/*
 * Quasar: lightweight threads and actors for the JVM.
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
/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.paralleluniverse.strands.channels.disruptor;

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.FixedSequenceGroup;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.Sequencer;
import com.lmax.disruptor.WaitStrategy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link SequenceBarrier} handed out for gating {@link EventProcessor}s on a cursor sequence and optional dependent {@link EventProcessor}(s),
 * using the given WaitStrategy.
 */
final class ProcessingSequenceBarrier implements SequenceBarrier {
    private final StrandBlockingWaitStrategy waitStrategy;
    private final Sequence dependentSequence;
    private volatile boolean alerted = false;
    private final Sequence cursorSequence;
    private final Sequencer sequencer;

    public ProcessingSequenceBarrier(final Sequencer sequencer,
            final WaitStrategy waitStrategy,
            final Sequence cursorSequence,
            final Sequence[] dependentSequences) {
        this.sequencer = sequencer;
        this.waitStrategy = (StrandBlockingWaitStrategy) waitStrategy;
        this.cursorSequence = cursorSequence;
        if (0 == dependentSequences.length) {
            dependentSequence = cursorSequence;
        } else {
            dependentSequence = new FixedSequenceGroup(dependentSequences);
        }
    }

    @Override
    public long waitFor(final long sequence)
            throws AlertException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long waitFor1(long sequence)
            throws AlertException, InterruptedException, SuspendExecution {
        checkAlert();

        long availableSequence = waitStrategy.waitFor1(sequence, cursorSequence, dependentSequence, this);

        if (availableSequence < sequence)
            return availableSequence;

        return sequencer.getHighestPublishedSequence(sequence, availableSequence);
    }

    @Override
    public long waitFor1(long sequence, long timeout, TimeUnit unit)
            throws AlertException, InterruptedException, SuspendExecution, TimeoutException {
        checkAlert();

        long availableSequence = waitStrategy.waitFor1(sequence, cursorSequence, dependentSequence, this, timeout, unit);

        if (availableSequence < sequence)
            return availableSequence;

        return sequencer.getHighestPublishedSequence(sequence, availableSequence);
    }

    @Override
    public long getCursor() {
        return dependentSequence.get();
    }

    @Override
    public boolean isAlerted() {
        return alerted;
    }

    @Override
    public void alert() {
        alerted = true;
        waitStrategy.signalAllWhenBlocking();
    }

    @Override
    public void clearAlert() {
        alerted = false;
    }

    @Override
    public void checkAlert() throws AlertException {
        if (alerted) {
            throw AlertException.INSTANCE;
        }
    }
}