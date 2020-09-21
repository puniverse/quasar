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
package co.paralleluniverse.fibers;

import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import static co.paralleluniverse.fibers.TestsHelper.exec;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class FiberOverheadJMHBenchmark {
    @Param({"3", "5", "10", "20"})
    public int DEPTH;

    @Param({"16", "100"})
    public int STACK;

    public static void main(String[] args) throws Exception {
        // Main.main(new String[]{"-usage"});
        new Runner(new OptionsBuilder()
                .include(FiberOverheadJMHBenchmark.class.getName() + ".*")
                .forks(1)
                .warmupTime(TimeValue.seconds(5))
                .warmupIterations(3)
                .measurementTime(TimeValue.seconds(5))
                .measurementIterations(5)
                //.addProfiler(GCProfiler.class)    // report GC time
                //.addProfiler(StackProfiler.class) // report method stack execution profile
                .build()).run();
    }

    @Benchmark
    public Object baseline() {
        res = 0;
        runnable.run();
        return res;
    }

    @Benchmark
    public Object fiber() {
        res = 0;
        exec(fiber);
        exec(fiber);
        fiber.reset();
        return res;
    }

    @Benchmark
    public Object fiberNoPark() {
        res = 0;
        exec(fiber2);
        fiber2.reset();
        return res;
    }

    private long res;
    private long rands[];
    private Runnable runnable;
    private Fiber fiber;
    private Fiber fiber2;

    @Setup
    public void preapre() {
        rands = new long[(DEPTH + 1) * 4];
        Random rnd = ThreadLocalRandom.current();
        for (int i = 0; i < rands.length; i++)
            rands[i] = rnd.nextLong();

        runnable = new Runnable() {
            @Override
            public void run() {
                res = recursive1(DEPTH);
            }
        };
        fiber = new Fiber((String) null, null, STACK, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                res = recursive2(DEPTH);
            }
        });
        fiber2 = new Fiber((String) null, null, STACK, new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                res = recursive3(DEPTH);
            }
        });
    }

    private long recursive1(int r) {
        long a = rands[(r << 2)];
        long b = rands[(r << 2) + 1];
        long c = rands[(r << 2) + 3];
        long res = r > 0 ? recursive1(r - 1) : rands[(r << 2) + 4];
        return a + b + c + res;
    }

    private long recursive2(int r) throws SuspendExecution {
        long a = rands[(r << 2)];
        long b = rands[(r << 2) + 1];
        long c = rands[(r << 2) + 3];
        long res;
        if (r > 0)
            res = recursive2(r - 1);
        else {
            Fiber.park();
            res = rands[(r << 2) + 4];
        }
        return a + b + c + res;
    }

    private long recursive3(int r) throws SuspendExecution {
        long a = rands[(r << 2)];
        long b = rands[(r << 2) + 1];
        long c = rands[(r << 2) + 3];
        long res;
        if (r > 0)
            res = recursive3(r - 1);
        else {
            nopark();
            res = rands[(r << 2) + 4];
        }
        return a + b + c + res;
    }

    private static void nopark() throws SuspendExecution {

    }
}
