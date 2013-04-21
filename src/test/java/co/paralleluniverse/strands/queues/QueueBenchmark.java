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

import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Martin Thompson
 * @autor pron
 */
public class QueueBenchmark {
    public static final int NUM_PRODUCERS = 3;
    public static final int QUEUE_CAPACITY = 32 * 1024;
    public static final int REPETITIONS = 50 * 1000 * 1000;
    public static final Integer TEST_VALUE = Integer.valueOf(777);

    public static void main(String[] args) throws Exception {
        timeQueue(1);   // SingleConsumerArrayObjectQueue
        timeQueue(2);   // SingleConsumerLinkedObjectQueue
        timeQueue(3);   // SingleConsumerLinkedArrayObjectQueue
        timeQueue(4);   // SingleConsumerArrayIntQueue
        timeQueue(5);   // SingleConsumerLinkedIntQueue
        timeQueue(6);   // SingleConsumerLinkedArrayIntQueue
        timeQueue(7);   // ArrayBlockingQueue
        timeQueue(8);   // LinkedBlockingQueue
        timeQueue(9);   // ConcurrentLinkedQueue
        timeQueue(10);  // LinkedTransferQueue
    }

    private static void timeQueue(int type) throws Exception {
        final Queue<Integer> queue = createQueue(type);
        if (queue == null)
            return;
        System.out.println("===== " + queue.getClass().getSimpleName() + ", " + NUM_PRODUCERS + " producers ===");
        for (int i = 0; i < 5; i++) {
            System.gc();
            System.gc();
            performanceRun(i, queue);
        }
    }

    private static Queue<Integer> createQueue(int type) {
        switch (type) {
            case 1:
                return new SingleConsumerArrayObjectQueue<Integer>(QUEUE_CAPACITY);
            case 2:
                return new SingleConsumerLinkedObjectQueue<Integer>();
            case 3:
                return new SingleConsumerLinkedArrayObjectQueue<Integer>();
            case 4:
                return new SingleConsumerArrayIntQueue(QUEUE_CAPACITY);
            case 5:
                return new SingleConsumerLinkedIntQueue();
            case 6:
                return new SingleConsumerLinkedArrayIntQueue();
            case 7:
                return new java.util.concurrent.ArrayBlockingQueue<Integer>(QUEUE_CAPACITY);
            case 8:
                return new java.util.concurrent.LinkedBlockingQueue<Integer>(QUEUE_CAPACITY);
            case 9:
                return new java.util.concurrent.ConcurrentLinkedQueue<Integer>();
            case 10:
                return new java.util.concurrent.LinkedTransferQueue<Integer>();

            default:
                throw new IllegalArgumentException("Invalid option: " + type);
        }
    }

    private static void performanceRun(final int runNumber, final Queue<Integer> queue) throws Exception {
        final long start = System.nanoTime();
        final int repetitions = (REPETITIONS / NUM_PRODUCERS) * NUM_PRODUCERS;
        final Thread[] producers = new Thread[NUM_PRODUCERS];

        for (int t = 0; t < NUM_PRODUCERS; t++) {
            producers[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    int i = REPETITIONS / NUM_PRODUCERS;
                    do {
                        while (!queue.offer(TEST_VALUE))
                            Thread.yield();
                    } while (0 != --i);
                }
            });
        }

        for (int t = 0; t < NUM_PRODUCERS; t++)
            producers[t].start();

        Integer result;
        int i = repetitions;
        do {
            while (null == (result = queue.poll()))
                Thread.yield();
        } while (0 != --i);

        for (int t = 0; t < NUM_PRODUCERS; t++)
            producers[t].join();

        final long duration = System.nanoTime() - start;
        final long ops = (repetitions * TimeUnit.SECONDS.toNanos(1)) / duration;
        System.out.format("%d - ops/sec=%,d - %s result=%d\n",
                Integer.valueOf(runNumber), Long.valueOf(ops),
                queue.getClass().getSimpleName(), result);
    }
}
