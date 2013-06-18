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

import co.paralleluniverse.concurrent.util.SingleConsumerNonblockingProducerDelayQueue;
import java.util.Queue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 *
 * @autor pron
 */
public class DelayQueueBenchmark {
    public static final int NUM_PRODUCERS = 5;
    public static final int REPETITIONS = 10 * 1000 * 1000;
    public static final Integer TEST_VALUE = Integer.valueOf(777);

    public static void main(String[] args) throws Exception {
        timeQueue(1);   // SingleConsumerArrayObjectQueue
        timeQueue(2);   // SingleConsumerLinkedObjectQueue
    }

    private static void timeQueue(int type) throws Exception {
        final Queue<DelayedValue> queue = createQueue(type);
        if (queue == null)
            return;
        System.out.println("===== " + queue.getClass().getSimpleName() + ", " + NUM_PRODUCERS + " producers ===");
        for (int i = 0; i < 5; i++) {
            System.gc();
            System.gc();
            performanceRun(i, queue);
        }
    }

    private static Queue<DelayedValue> createQueue(int type) {
        switch (type) {
            case 1:
                return new DelayQueue<DelayedValue>();
            case 2:
                return new SingleConsumerNonblockingProducerDelayQueue<DelayedValue>();

            default:
                throw new IllegalArgumentException("Invalid option: " + type);
        }
    }


    private static void performanceRun(final int runNumber, final Queue<DelayedValue> queue) throws Exception {
        final long start = System.nanoTime();
        final int repetitions = (REPETITIONS / NUM_PRODUCERS) * NUM_PRODUCERS;
        final Thread[] producers = new Thread[NUM_PRODUCERS];

        for (int t = 0; t < NUM_PRODUCERS; t++) {
            producers[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    final ThreadLocalRandom rand = ThreadLocalRandom.current();
                    int i = REPETITIONS / NUM_PRODUCERS;
                    do {
                        while (!queue.offer(new DelayedValue(TEST_VALUE, rand.nextInt(0, 11))))
                            Thread.yield();
                    } while (0 != --i);
                }
            });
        }

        for (int t = 0; t < NUM_PRODUCERS; t++)
            producers[t].start();

        DelayedValue result;
        int i = repetitions;
        do {
            while (null == (result = queue.poll()))
                Thread.yield();
        } while (0 != --i);

        for (int t = 0; t < NUM_PRODUCERS; t++)
            producers[t].join();

        final long duration = System.nanoTime() - start;
        final long ops = (repetitions * TimeUnit.SECONDS.toNanos(1)) / duration;
        System.out.format("%d - ops/sec=%,d - %s result=%s\n",
                Integer.valueOf(runNumber), Long.valueOf(ops),
                queue.getClass().getSimpleName(), result);
    }
    
}
