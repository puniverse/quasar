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
package co.paralleluniverse.fibers;

import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.parameters.TimeValue;
import org.openjdk.jmh.profile.*;
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

    @GenerateMicroBenchmark
    public Object baseline() {
        res = 0;
        runnable.run();
        return res;
    }

    @GenerateMicroBenchmark
    public Object fiber() {
        res = 0;
        exec(fiber);
        exec(fiber);
        return res;
    }

    private long res;
    private long rands[];
    private Runnable runnable;
    private Fiber fiber;

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
            res = recursive1(r - 1);
        else {
            Fiber.park();
            res = rands[(r << 2) + 4];
        }
        return a + b + c + res;
    }
}


//# Run complete. Total time: 00:11:16
//
//Benchmark                                  (DEPTH) (STACK)   Mode   Samples        Score  Score error    Units
//c.p.f.FiberOverheadJMHBenchmark.baseline         3      16   avgt         5       17.278        0.480    ns/op
//c.p.f.FiberOverheadJMHBenchmark.baseline         3     100   avgt         5       18.303        4.226    ns/op
//c.p.f.FiberOverheadJMHBenchmark.baseline         5      16   avgt         5       24.707        3.141    ns/op
//c.p.f.FiberOverheadJMHBenchmark.baseline         5     100   avgt         5       24.095        3.880    ns/op
//c.p.f.FiberOverheadJMHBenchmark.baseline        10      16   avgt         5       41.161        3.055    ns/op
//c.p.f.FiberOverheadJMHBenchmark.baseline        10     100   avgt         5       39.576        1.520    ns/op
//c.p.f.FiberOverheadJMHBenchmark.baseline        20      16   avgt         5       69.398        7.747    ns/op
//c.p.f.FiberOverheadJMHBenchmark.baseline        20     100   avgt         5       69.023        1.179    ns/op
//c.p.f.FiberOverheadJMHBenchmark.fiber            3      16   avgt         5      234.897        5.489    ns/op
//c.p.f.FiberOverheadJMHBenchmark.fiber            3     100   avgt         5      239.196       17.656    ns/op
//c.p.f.FiberOverheadJMHBenchmark.fiber            5      16   avgt         5      239.292        5.923    ns/op
//c.p.f.FiberOverheadJMHBenchmark.fiber            5     100   avgt         5      242.205       14.829    ns/op
//c.p.f.FiberOverheadJMHBenchmark.fiber           10      16   avgt         5      264.956        7.680    ns/op
//c.p.f.FiberOverheadJMHBenchmark.fiber           10     100   avgt         5      282.650       46.519    ns/op
//c.p.f.FiberOverheadJMHBenchmark.fiber           20      16   avgt         5      370.537       42.557    ns/op
//c.p.f.FiberOverheadJMHBenchmark.fiber           20     100   avgt         5      315.896       10.556    ns/op
//
