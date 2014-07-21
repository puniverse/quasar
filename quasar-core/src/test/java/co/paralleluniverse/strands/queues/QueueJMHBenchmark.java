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
package co.paralleluniverse.strands.queues;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Control;

/**
 *
 * @author Martin Thompson
 * @autor pron
 */
public class QueueJMHBenchmark {
    /*
     * See: 
     * http://psy-lob-saw.blogspot.co.il/2013/04/writing-java-micro-benchmarks-with-jmh.html
     * http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/
     */
    private static final String BENCHMARK = QueueJMHBenchmark.class.getName() + ".*";

    public static void main(String[] args) throws Exception {
        // Main.main(new String[]{"-usage"});
        Main.main(buildArguments(BENCHMARK, 5, 5000, 3));
    }

    private static String[] buildArguments(String className, int nRuns, int runForMilliseconds, int nProducers) {
        return new String[]{className,
                    "-f", "1",
                    "-i", "" + nRuns,
                    "-r", runForMilliseconds + "ms",
                    "-tg", "1," + nProducers,
                    "-w", "5000ms",
                    "-wi", "3",};
    }
    private static final int QUEUE_CAPACITY = 32 * 1024;
    private static final Integer TEST_VALUE = Integer.valueOf(777);

    @State(Scope.Group)
    public static class Q {
        Queue<Integer> singleConsumerArrayObjectQueue = new SingleConsumerArrayObjectQueue<Integer>(QUEUE_CAPACITY);
        Queue<Integer> singleConsumerLinkedObjectQueue = new SingleConsumerLinkedObjectQueue<Integer>();
        Queue<Integer> singleConsumerLinkedArrayObjectQueue = new SingleConsumerLinkedArrayObjectQueue<Integer>();
        Queue<Integer> singleConsumerArrayIntQueue = new SingleConsumerArrayIntQueue(QUEUE_CAPACITY);
        Queue<Integer> singleConsumerLinkedIntQueue = new SingleConsumerLinkedIntQueue();
        Queue<Integer> singleConsumerLinkedArrayIntQueue = new SingleConsumerLinkedArrayIntQueue();
        Queue<Integer> arrayBlockingQueue = new ArrayBlockingQueue<Integer>(QUEUE_CAPACITY);
        Queue<Integer> linkedBlockingQueue = new LinkedBlockingQueue<Integer>(QUEUE_CAPACITY);
        Queue<Integer> concurrentLinkedQueue = new ConcurrentLinkedQueue<Integer>();
        Queue<Integer> linkedTransferQueue = new LinkedTransferQueue<Integer>();
    }

    public void write(Control cnt, Queue<Integer> queue) {
        while (!cnt.stopMeasurement && !queue.offer(TEST_VALUE))
            Thread.yield();
    }

    public Integer read(Control cnt, Queue<Integer> queue) {
        Integer result = null;
        while (!cnt.stopMeasurement && null == (result = queue.poll()))
            Thread.yield();
        return result;
    }

    // it is important that "read" is lexicographically lower than "write", as this is the order specified in the -tg flag
    @Benchmark
    @Group("singleConsumerArrayObjectQueue")
    public Object read_singleConsumerArrayObjectQueue(Control cnt, Q q) {
        return read(cnt, q.singleConsumerArrayObjectQueue);
    }

    @Benchmark
    @Group("singleConsumerArrayObjectQueue")
    public void write_singleConsumerArrayObjectQueue(Control cnt, Q q) {
        write(cnt, q.singleConsumerArrayObjectQueue);
    }

    @Benchmark
    @Group("singleConsumerLinkedObjectQueue")
    public Object read_SingleConsumerLinkedObjectQueue(Control cnt, Q q) {
        return read(cnt, q.singleConsumerLinkedObjectQueue);
    }

    @Benchmark
    @Group("singleConsumerLinkedObjectQueue")
    public void write_SingleConsumerLinkedObjectQueue(Control cnt, Q q) {
        write(cnt, q.singleConsumerLinkedObjectQueue);
    }

    @Benchmark
    @Group("singleConsumerLinkedArrayObjectQueue")
    public Object read_SingleConsumerLinkedArrayObjectQueue(Control cnt, Q q) {
        return read(cnt, q.singleConsumerLinkedArrayObjectQueue);
    }

    @Benchmark
    @Group("singleConsumerLinkedArrayObjectQueue")
    public void write_SingleConsumerLinkedArrayObjectQueue(Control cnt, Q q) {
        write(cnt, q.singleConsumerLinkedArrayObjectQueue);
    }

    @Benchmark
    @Group("singleConsumerArrayIntQueue")
    public Object read_SingleConsumerArrayIntQueue(Control cnt, Q q) {
        return read(cnt, q.singleConsumerArrayIntQueue);
    }

    @Benchmark
    @Group("singleConsumerArrayIntQueue")
    public void write_SingleConsumerArrayIntQueue(Control cnt, Q q) {
        write(cnt, q.singleConsumerArrayIntQueue);
    }

    @Benchmark
    @Group("singleConsumerLinkedIntQueue")
    public Object read_SingleConsumerLinkedIntQueue(Control cnt, Q q) {
        return read(cnt, q.singleConsumerLinkedIntQueue);
    }

    @Benchmark
    @Group("singleConsumerLinkedIntQueue")
    public void write_SingleConsumerLinkedIntQueue(Control cnt, Q q) {
        write(cnt, q.singleConsumerLinkedIntQueue);
    }

    @Benchmark
    @Group("singleConsumerLinkedArrayIntQueue")
    public Object read_SingleConsumerLinkedArrayIntQueue(Control cnt, Q q) {
        return read(cnt, q.singleConsumerLinkedArrayIntQueue);
    }

    @Benchmark
    @Group("singleConsumerLinkedArrayIntQueue")
    public void write_SingleConsumerLinkedArrayIntQueue(Control cnt, Q q) {
        write(cnt, q.singleConsumerLinkedArrayIntQueue);
    }

    @Benchmark
    @Group("arrayBlockingQueue")
    public Object read_ArrayBlockingQueue(Control cnt, Q q) {
        return read(cnt, q.arrayBlockingQueue);
    }

    @Benchmark
    @Group("arrayBlockingQueue")
    public void write_ArrayBlockingQueue(Control cnt, Q q) {
        write(cnt, q.arrayBlockingQueue);
    }

    @Benchmark
    @Group("linkedBlockingQueue")
    public Object read_LinkedBlockingQueue(Control cnt, Q q) {
        return read(cnt, q.linkedBlockingQueue);
    }

    @Benchmark
    @Group("linkedBlockingQueue")
    public void write_LinkedBlockingQueue(Control cnt, Q q) {
        write(cnt, q.linkedBlockingQueue);
    }

    @Benchmark
    @Group("concurrentLinkedQueue")
    public Object read_ConcurrentLinkedQueue(Control cnt, Q q) {
        return read(cnt, q.concurrentLinkedQueue);
    }

    @Benchmark
    @Group("concurrentLinkedQueue")
    public void write_ConcurrentLinkedQueue(Control cnt, Q q) {
        write(cnt, q.concurrentLinkedQueue);
    }

    @Benchmark
    @Group("linkedTransferQueue")
    public Object read_LinkedTransferQueue(Control cnt, Q q) {
        return read(cnt, q.linkedTransferQueue);
    }

    @Benchmark
    @Group("linkedTransferQueue")
    public void write_LinkedTransferQueue(Control cnt, Q q) {
        write(cnt, q.linkedTransferQueue);
    }
}
