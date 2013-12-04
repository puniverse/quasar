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

import java.util.Queue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ThreadLocalRandom;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.Control;

/**
 *
 * @autor pron
 */
public class DelayQueueJMHBenchmark {
    /*
     * See: 
     * http://psy-lob-saw.blogspot.co.il/2013/04/writing-java-micro-benchmarks-with-jmh.html
     * http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/
     */
    private static final String BENCHMARK = DelayQueueJMHBenchmark.class.getName() + ".*";

    public static void main(String[] args) throws Exception {
        // Main.main(new String[]{"-usage"});
        Main.main(buildArguments(BENCHMARK, 5, 5000, 1));
    }

    private static String[] buildArguments(String className, int nRuns, int runForMilliseconds, int nProducers) {
        return new String[]{className,
                    "-f", "1",
                    "-i", "" + nRuns,
                    "-r", runForMilliseconds + "ms",
                    "-tg", "1," + nProducers,
                    "-w", "5000ms",
                    "-wi", "3",
                    //"--jvmargs", "-Xmx1024m",
                    "-prof", "stack,hs_rt,hs_thr,gc"
                };
    }
    public static final Integer TEST_VALUE = Integer.valueOf(777);

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
    }

    @State(Scope.Group)
    public static class Q {
        Queue<DelayedValue> delayQueue = new DelayQueue<DelayedValue>();
        Queue<DelayedValue> singleConsumerNonblockingProducerDelayQueue = new SingleConsumerNonblockingProducerDelayQueue<DelayedValue>();
    }

    public void write(Control cnt, BenchmarkState b, Queue<DelayedValue> queue) {
        while (!cnt.stopMeasurement && !queue.offer(new DelayedValue(TEST_VALUE, b.rand.nextInt(0, 11))))
            Thread.yield();
    }

    public DelayedValue read(Control cnt, Queue<DelayedValue> queue) {
        DelayedValue result = null;
        while (!cnt.stopMeasurement && null == (result = queue.poll()))
            Thread.yield();
        return result;
    }

    // it is important that "read" is lexicographically lower than "write", as this is the order specified in the -tg flag
    @GenerateMicroBenchmark
    @Group("delayQueue")
    public Object read_DelayQueue(Control cnt, BenchmarkState b, Q q) {
        return read(cnt, q.delayQueue);
    }

    @GenerateMicroBenchmark
    @Group("delayQueue")
    public void write_DelayQueue(Control cnt, BenchmarkState b, Q q) {
        write(cnt, b, q.delayQueue);
    }

    @GenerateMicroBenchmark
    @Group("singleConsumerNonblockingProducerDelayQueue")
    public Object read_SingleConsumerLinkedObjectQueue(Control cnt, BenchmarkState b, Q q) {
        return read(cnt, q.singleConsumerNonblockingProducerDelayQueue);
    }

    @GenerateMicroBenchmark
    @Group("singleConsumerNonblockingProducerDelayQueue")
    public void write_SingleConsumerLinkedObjectQueue(Control cnt, BenchmarkState b, Q q) {
        write(cnt, b, q.singleConsumerNonblockingProducerDelayQueue);
    }
}
