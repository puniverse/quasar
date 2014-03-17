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
package co.paralleluniverse.strands;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ThreadsBenchmark {
    static final int RINGS = 2;
    static final int THREADS_PER_RING = 100;
    static final int MESSAGES_PER_RING = 10000;
    static final int bufferSize = 10;

    private static final ExecutorService exec = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).build());
    public static void main(String args[]) throws Exception {
        System.out.println("COMPILER: " + System.getProperty("java.vm.name"));
        System.out.println("VERSION: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("PROCESSORS: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        System.out.println("RINGS: " + RINGS);
        System.out.println("THREADS_PER_RING: " + THREADS_PER_RING);
        System.out.println("MESSAGES_PER_RING: " + MESSAGES_PER_RING);

        for (int i = 0; i < 3; i++) {
            System.out.println("\nRun: " + i);
            new ThreadsBenchmark().run();
        }
    }

    volatile Object blackHole;

    void run() throws ExecutionException, InterruptedException {
        final long start = System.nanoTime();

        List<Future<?>> ringLeaders = new ArrayList<>();
        for (int i = 0; i < RINGS; i++)
            ringLeaders.add(exec.submit(createRing(THREADS_PER_RING, MESSAGES_PER_RING)));

        final long afterInit = System.nanoTime();

        final long afterStart = System.nanoTime();

        for (Future<?> leader : ringLeaders)
            leader.get();

        final long end = System.nanoTime();

        System.out.println("Init time (ms): " + TimeUnit.MILLISECONDS.convert(afterInit - start, TimeUnit.NANOSECONDS));
        System.out.println("Start time (ms): " + TimeUnit.MILLISECONDS.convert(afterStart - afterInit, TimeUnit.NANOSECONDS));
        System.out.println("Running time (ms): " + TimeUnit.MILLISECONDS.convert(end - afterStart, TimeUnit.NANOSECONDS));
        System.out.println("Total time (ms): " + TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS));
        System.out.println("Black hole: " + blackHole);
    }

    Runnable createRing(int size, final int rounds) {
        final BlockingQueue<String> firstChannel = new ArrayBlockingQueue<String>(bufferSize);

        BlockingQueue<String> c = firstChannel;
        for (int i = 0; i < size - 1; i++)
            c = createRelayStrand(c);

        final BlockingQueue<String> lastChannel = c;

        Runnable ringLeader = new Runnable() {
            @Override
            public void run() {
                try {
                    lastChannel.add("number:"); // start things off

                    String m = null;
                    for (int i = 0; i < rounds; i++) {
                        m = firstChannel.take();
                        lastChannel.add(createMessage(m));
                    }
                    lastChannel.add("stop");
                    blackHole = m;
                } catch (InterruptedException e) {
                }
            }
        };
        return ringLeader;
    }

    BlockingQueue<String> createRelayStrand(final BlockingQueue<String> prev) {
        final BlockingQueue<String> channel = new ArrayBlockingQueue<String>(bufferSize);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    String m;
                    while ((m = channel.take()) != "stop")
                        prev.add(createMessage(m));
                } catch (InterruptedException e) {

                }
            }
        };
        exec.execute(r);
        return channel;
    }

    String createMessage(String receivedMessage) {
        return receivedMessage.substring(0, 7) + ThreadLocalRandom.current().nextInt();
    }
}
