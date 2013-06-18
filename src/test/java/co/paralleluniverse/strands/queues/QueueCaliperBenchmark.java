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
package co.paralleluniverse.strands.queues;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.runner.CaliperMain;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

/**
 *
 * @author Martin Thompson
 * @autor pron
 */
public class QueueCaliperBenchmark extends Benchmark {
    public static void main(String[] args) throws Exception {
        CaliperMain.main(QueueCaliperBenchmark.class, args);
    }
    
    private static final int QUEUE_CAPACITY = 32 * 1024;
    private static final Integer TEST_VALUE = Integer.valueOf(777);
    //
    @Param({"1", "2", "5", "10"})
    private int numProducers = 3;
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

    private Integer timeQueue(final int reps, final Queue<Integer> queue) {
        final int repsPerProducer = Math.max(reps / numProducers, 1);
        final int repetitions = repsPerProducer * numProducers;

        for (int t = 0; t < numProducers; t++) {
            producers[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    int i = repsPerProducer;
                    do {
                        while (!queue.offer(TEST_VALUE))
                            Thread.yield();
                    } while (0 != --i);
                }
            });
        }

        for (int t = 0; t < numProducers; t++)
            producers[t].start();

        Integer result;
        int i = repetitions;
        do {
            while (null == (result = queue.poll()))
                Thread.yield();
        } while (0 != --i);

        return result;
    }

    public Object timeSingleConsumerArrayObjectQueue(int reps) {
        return timeQueue(reps, new SingleConsumerArrayObjectQueue<Integer>(QUEUE_CAPACITY));
    }

    public Object timeSingleConsumerLinkedObjectQueue(int reps) {
        return timeQueue(reps, new SingleConsumerLinkedObjectQueue<Integer>());
    }

    public Object timeSingleConsumerLinkedArrayObjectQueue(int reps) {
        return timeQueue(reps, new SingleConsumerLinkedArrayObjectQueue<Integer>());
    }

    public Object timeSingleConsumerArrayIntQueue(int reps) {
        return timeQueue(reps, new SingleConsumerArrayIntQueue(QUEUE_CAPACITY));
    }

    public Object timeSingleConsumerLinkedIntQueue(int reps) {
        return timeQueue(reps, new SingleConsumerLinkedIntQueue());
    }

    public Object timeSingleConsumerLinkedArrayIntQueue(int reps) {
        return timeQueue(reps, new SingleConsumerLinkedArrayIntQueue());
    }

    public Object timeArrayBlockingQueue(int reps) {
        return timeQueue(reps, new ArrayBlockingQueue<Integer>(QUEUE_CAPACITY));
    }

    public Object timeLinkedBlockingQueue(int reps) {
        return timeQueue(reps, new LinkedBlockingQueue<Integer>(QUEUE_CAPACITY));
    }

    public Object timeConcurrentLinkedQueue(int reps) {
        return timeQueue(reps, new ConcurrentLinkedQueue<Integer>());
    }

    public Object timeLinkedTransferQueue(int reps) {
        return timeQueue(reps, new LinkedTransferQueue<Integer>());
    }
}
