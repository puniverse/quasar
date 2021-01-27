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

import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class StrandsBenchmark {
    static final boolean HEAVYWEIGHT = false;
    static final int RINGS = 2;
    static final int STRANDS_PER_RING = 100;
    static final int MESSAGES_PER_RING = 10000;
    static final int bufferSize = 10;

    public static void main(String args[]) throws Exception {
        System.out.println("COMPILER: " + System.getProperty("java.vm.name"));
        System.out.println("VERSION: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("PROCESSORS: " + Runtime.getRuntime().availableProcessors());
        System.out.println("FORK_JOIN: " + Debug.whereIs(ForkJoinPool.class));
        System.out.println();

        System.out.println("HEAVYWEIGHT: " + HEAVYWEIGHT);
        System.out.println("RINGS: " + RINGS);
        System.out.println("STRANDS_PER_RING: " + STRANDS_PER_RING);
        System.out.println("MESSAGES_PER_RING: " + MESSAGES_PER_RING);

        for (int i = 0; i < 3; i++) {
            System.out.println("\nRun: " + i);
            new StrandsBenchmark().run();
        }
    }

    volatile Object blackHole;

    void run() throws ExecutionException, InterruptedException {
        final long start = System.nanoTime();

        List<Strand> ringLeaders = new ArrayList<>();
        for (int i = 0; i < RINGS; i++)
            ringLeaders.add(createRing(HEAVYWEIGHT, STRANDS_PER_RING, MESSAGES_PER_RING));

        final long afterInit = System.nanoTime();

        for (Strand leader : ringLeaders)
            leader.start();

        final long afterStart = System.nanoTime();

        for (Strand leader : ringLeaders)
            leader.join();

        final long end = System.nanoTime();

        System.out.println("Init time (ms): " + TimeUnit.MILLISECONDS.convert(afterInit - start, TimeUnit.NANOSECONDS));
        System.out.println("Start time (ms): " + TimeUnit.MILLISECONDS.convert(afterStart - afterInit, TimeUnit.NANOSECONDS));
        System.out.println("Running time (ms): " + TimeUnit.MILLISECONDS.convert(end - afterStart, TimeUnit.NANOSECONDS));
        System.out.println("Total time (ms): " + TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS));
        System.out.println("Black hole: " + blackHole);
    }

    Strand createRing(boolean heavyweight, int size, final int rounds) {
        final Channel<String> firstChannel = Channels.newChannel(bufferSize);

        Channel<String> c = firstChannel;
        for (int i = 0; i < size - 1; i++)
            c = createRelayStrand(heavyweight, c);

        final Channel<String> lastChannel = c;

        Strand ringLeader = newStrand(heavyweight, new SuspendableRunnable() {

            @Override
            public void run() throws SuspendExecution, InterruptedException {
                lastChannel.send("number:"); // start things off

                String m = null;
                for (int i = 0; i < rounds; i++) {
                    m = firstChannel.receive();
                    lastChannel.send(createMessage(m));
                }
                lastChannel.close();
                blackHole = m;
            }
        });
        return ringLeader;
    }

    Channel<String> createRelayStrand(boolean heavyweight, final Channel<String> prev) {
        final Channel<String> channel = Channels.newChannel(bufferSize);
        Strand s = newStrand(heavyweight, new SuspendableRunnable() {
            @Override
            public void run() throws InterruptedException, SuspendExecution {
                String m;
                while ((m = channel.receive()) != null)
                    prev.send(createMessage(m));
                prev.close();
            }
        });
        s.start();
        return channel;
    }

    Strand newStrand(boolean heavyweight, SuspendableRunnable target) {
        if (heavyweight)
            return Strand.of(new Thread(Strand.toRunnable(target)));
        else
            return Strand.of(new Fiber(target));
    }

    String createMessage(String receivedMessage) {
        return receivedMessage.substring(0, 7) + ThreadLocalRandom.current().nextInt();
    }
}
