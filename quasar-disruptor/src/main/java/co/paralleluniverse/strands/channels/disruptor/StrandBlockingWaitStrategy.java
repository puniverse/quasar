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
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.WaitStrategy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Blocking strategy that uses a lock and condition variable for {@link EventProcessor}s waiting on a barrier.
 */
public final class StrandBlockingWaitStrategy implements WaitStrategy {
    private final SimpleConditionSynchronizer processorNotifyCondition = new SimpleConditionSynchronizer(this);

    @Override
    public long waitFor(long sequence, Sequence cursorSequence, Sequence dependentSequence, com.lmax.disruptor.SequenceBarrier barrier)
            throws AlertException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    public long waitFor1(long sequence, Sequence cursorSequence, Sequence dependentSequence, com.lmax.disruptor.SequenceBarrier barrier)
            throws AlertException, InterruptedException, SuspendExecution {
        long availableSequence;
        if ((availableSequence = cursorSequence.get()) < sequence) {
            Object token = processorNotifyCondition.register();
            try {
                for (int i = 0; (availableSequence = cursorSequence.get()) < sequence; i++) {
                    barrier.checkAlert();
                    processorNotifyCondition.await(i);
                }
            } finally {
                processorNotifyCondition.unregister(token);
            }
        }

        while ((availableSequence = dependentSequence.get()) < sequence) {
            barrier.checkAlert();
        }

        return availableSequence;
    }

    public long waitFor1(long sequence, Sequence cursorSequence, Sequence dependentSequence, com.lmax.disruptor.SequenceBarrier barrier, long timeout, TimeUnit unit)
            throws AlertException, InterruptedException, SuspendExecution, TimeoutException {
        long availableSequence;
        if ((availableSequence = cursorSequence.get()) < sequence) {
            long left = unit.toNanos(timeout);
            final long deadline = System.nanoTime() + left;

            Object token = processorNotifyCondition.register();
            try {
                for (int i=0; (availableSequence = cursorSequence.get()) < sequence; i++) {
                    barrier.checkAlert();
                    processorNotifyCondition.await(i, left, TimeUnit.NANOSECONDS);
                    left = deadline - System.nanoTime();
                    if (left <= 0)
                        throw new TimeoutException();
                }
            } finally {
                processorNotifyCondition.unregister(token);
            }
        }

        while ((availableSequence = dependentSequence.get()) < sequence) {
            barrier.checkAlert();
        }

        return availableSequence;
    }

    @Override
    public void signalAllWhenBlocking() {
        processorNotifyCondition.signalAll();
    }
}
