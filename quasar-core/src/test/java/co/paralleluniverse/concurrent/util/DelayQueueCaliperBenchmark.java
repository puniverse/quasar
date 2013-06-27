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
/*
 * Copyright 2012 Real Logic Ltd.
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
package co.paralleluniverse.concurrent.util;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.runner.CaliperMain;
import java.util.Queue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @autor pron
 */
public class DelayQueueCaliperBenchmark extends Benchmark {
    public static void main(String[] args) throws Exception {
        CaliperMain.main(DelayQueueCaliperBenchmark.class, args);
    }
    public static final Integer TEST_VALUE = Integer.valueOf(777);
    //
    @Param({"1", "2", "5", "10"})
    private int numProducers;
    private Thread[] producers;

    @Override
    protected void setUp() throws Exception {
        producers = new Thread[numProducers];
    }

    @Override
    protected void tearDown() throws Exception {
        for (int t = 0; t < numProducers; t++)
            producers[t].join();
    }

    private Object timeQueue(final int reps, final Queue<DelayedValue> queue) {
        final int repsPerProducer = Math.max(reps / numProducers, 1);
        final int repetitions = repsPerProducer * numProducers;

        for (int t = 0; t < numProducers; t++) {
            producers[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    final ThreadLocalRandom rand = ThreadLocalRandom.current();
                    int i = repsPerProducer;
                    do {
                        while (!queue.offer(new DelayedValue(TEST_VALUE, rand.nextInt(0, 11))))
                            Thread.yield();
                    } while (0 != --i);
                }
            });
        }

        for (int t = 0; t < numProducers; t++)
            producers[t].start();

        DelayedValue result;
        int i = repetitions;
        do {
            while (null == (result = queue.poll()))
                Thread.yield();
        } while (0 != --i);

        return result;
    }

    public Object timeDelayQueue(int reps) {
        return timeQueue(reps, new DelayQueue<DelayedValue>());
    }

    public Object timeSingleConsumerLinkedObjectQueue(int reps) {
        return timeQueue(reps, new SingleConsumerNonblockingProducerDelayQueue<DelayedValue>());
    }
}
