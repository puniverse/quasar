/*
 * Copyright (C) 2011-2013, Parallel Universe Software Co. All rights reserved.
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

import com.codahale.metrics.Gauge;
import static com.codahale.metrics.MetricRegistry.name;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public class MetricsForkJoinPoolMonitor extends ForkJoinPoolMonitor {
    public MetricsForkJoinPoolMonitor(String name, ForkJoinPool fjPool) {
        super(name, fjPool);
        Metrics.register(metric(name, "status"), new Gauge<Status>() {
            @Override
            public Status getValue() {
                final ForkJoinPool fjPool = fjPool();
                if (fjPool.isTerminated()) // Returns true if all tasks have completed following shut down.
                    return ForkJoinPoolMonitor.Status.TERMINATED;
                if (fjPool.isTerminating()) // Returns true if the process of termination has commenced but not yet completed.
                    return ForkJoinPoolMonitor.Status.TERMINATING;
                if (fjPool.isShutdown()) // Returns true if this pool has been shut down.
                    return ForkJoinPoolMonitor.Status.SHUTDOWN;
                if (fjPool.isQuiescent()) // Returns true if all worker threads are currently idle.
                    return ForkJoinPoolMonitor.Status.QUIESCENT;
                return ForkJoinPoolMonitor.Status.ACTIVE;
            }
        });

        Metrics.register(metric(name, "asyncMode"), new Gauge<Boolean>() {
            @Override
            public Boolean getValue() {
                return fjPool().getAsyncMode();
            }
        });

        Metrics.register(metric(name, "parallelism"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return fjPool().getParallelism(); // Returns the targeted parallelism level of this pool.
            }
        });

        Metrics.register(metric(name, "poolSize"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return fjPool().getPoolSize(); // Returns the number of worker threads that have started but not yet terminated.
            }
        });

        Metrics.register(metric(name, "activeThreadCount"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return fjPool().getActiveThreadCount();
            }
        });

        Metrics.register(metric(name, "runningThreadCount"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return fjPool().getRunningThreadCount();
            }
        });

        Metrics.register(metric(name, "queuedSubmissionCount"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return fjPool().getQueuedSubmissionCount();
            }
        });

        Metrics.register(metric(name, "queuedTaskCount"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return fjPool().getQueuedTaskCount();
            }
        });

        Metrics.register(metric(name, "stealCount"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return fjPool().getStealCount();
            }
        });

        Metrics.register(metric(name, "latency"), new Gauge<Long[]>() {
            @Override
            public Long[] getValue() {
                long[] res = new ExecutorServiceLatencyProbe(fjPool(), 5).fire();
                Long[] ret = new Long[res.length];
                for (int i = 0; i < res.length; i++)
                    ret[i] = res[i];
                return ret;
            }
        });
    }

    protected final String metric(String poolName, String name) {
        return name("co.paralleluniverse", "fjPool", poolName, name);
    }

    @Override
    protected ForkJoinPool fjPool() {
        final ForkJoinPool fjPool = super.fjPool();
        if (fjPool == null) {
            unregister();
            throw new RuntimeException("Pool collected");
        }
        return fjPool;
    }
}
