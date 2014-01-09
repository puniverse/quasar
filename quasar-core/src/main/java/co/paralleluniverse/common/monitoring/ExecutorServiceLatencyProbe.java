/*
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
package co.paralleluniverse.common.monitoring;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class ExecutorServiceLatencyProbe {
    private final ExecutorService executor;
    private final int numProbes;

    public ExecutorServiceLatencyProbe(ExecutorService executor, int numProbes) {
        this.executor = executor;
        this.numProbes = numProbes;
    }

    public long[] fire() {
        final Callable<Long> task = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return System.nanoTime();
            }
        };

        final long[] start = new long[numProbes];
        final Future<Long>[] futs = new Future[numProbes];
        
        for (int i = 0; i < numProbes; i++) {
            start[i] = System.nanoTime();
            futs[i] = executor.submit(task);
        }

        final long[] res = new long[numProbes];
        Arrays.fill(res, -1);
        try {
            for (int i = 0; i < numProbes; i++) {
                final long nanos = futs[i].get() - start[i];
                res[i] = TimeUnit.MICROSECONDS.convert(nanos, TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new AssertionError(e);
        }
        return res;
    }
}
