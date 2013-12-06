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
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.monitoring.ForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.JMXForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.MetricsForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.MonitorType;
import co.paralleluniverse.concurrent.forkjoin.ExtendedForkJoinWorkerFactory;
import co.paralleluniverse.concurrent.forkjoin.ExtendedForkJoinWorkerThread;
import co.paralleluniverse.concurrent.forkjoin.MonitoredForkJoinPool;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jsr166e.ConcurrentHashMapV8;
import jsr166e.ForkJoinPool;

/**
 * A thread-pool based scheduler for fibers. Internally, this scheduler uses a {@code ForkJoinPool} to schedule fiber execution.
 *
 * @author pron
 */
public class FiberScheduler {
    static final FibersMonitor NOOP_FIBERS_MONITOR = new NoopFibersMonitor();
    private final ForkJoinPool fjPool;
    private final FiberTimedScheduler timer;
    private final FibersMonitor fibersMonitor;
    private final Set<FiberWorkerThread> activeThreads = Collections.newSetFromMap(new ConcurrentHashMapV8<FiberWorkerThread, Boolean>());

    /**
     * Creates a new fiber scheduler.
     *
     * @param name             the scheuler's name. This name is used in naming the scheduler's threads.
     * @param parallelism      the number of threads in the pool
     * @param exceptionHandler an {@link UncaughtExceptionHandler UncaughtExceptionHandler} to be used for exceptions thrown in fibers that aren't caught.
     * @param monitorType      the {@link MonitorType} type to use for the {@code ForkJoinPool}.
     * @param detailedInfo     whether detailed information about the fibers is collected by the fibers monitor.
     */
    public FiberScheduler(String name, int parallelism, UncaughtExceptionHandler exceptionHandler, MonitorType monitorType, boolean detailedInfo) {
        this.fjPool = createForkJoinPool(name, parallelism, exceptionHandler, monitorType);

        if (fjPool instanceof MonitoredForkJoinPool && ((MonitoredForkJoinPool) fjPool).getMonitor() != null)
            this.fibersMonitor = new JMXFibersMonitor(((MonitoredForkJoinPool) fjPool).getName(), this, detailedInfo);
        else
            this.fibersMonitor = NOOP_FIBERS_MONITOR;

        this.timer = createTimer(fjPool, fibersMonitor);
    }

    /**
     * Creates a new fiber scheduler using a default {@link UncaughtExceptionHandler UncaughtExceptionHandler}.
     *
     * @param name             the scheuler's name. This name is used in naming the scheduler's threads.
     * @param parallelism      the number of threads in the pool
     * @param monitorType      the {@link MonitorType} type to use for the {@code ForkJoinPool}.
     * @param detailedInfo     whether detailed information about the fibers is collected by the fibers monitor.
     */
    public FiberScheduler(String name, int parallelism, MonitorType monitorType, boolean detailedInfo) {
        this(name, parallelism, null, monitorType, detailedInfo);
    }

    private FiberScheduler(ForkJoinPool fjPool, FiberTimedScheduler timeService, boolean detailedInfo) {
        if (!fjPool.getAsyncMode())
            throw new IllegalArgumentException("ForkJoinPool is not async");
        this.fjPool = fjPool;

        if (fjPool instanceof MonitoredForkJoinPool && ((MonitoredForkJoinPool) fjPool).getMonitor() != null)
            this.fibersMonitor = new JMXFibersMonitor(((MonitoredForkJoinPool) fjPool).getName(), this, detailedInfo);
        else
            this.fibersMonitor = NOOP_FIBERS_MONITOR;

        this.timer = timeService != null ? timeService : createTimer(fjPool, fibersMonitor);
    }

    private ForkJoinPool createForkJoinPool(String name, int parallelism, Thread.UncaughtExceptionHandler exceptionHandler, MonitorType monitorType) {
        final MonitoredForkJoinPool pool = new MonitoredForkJoinPool(name, parallelism, new ExtendedForkJoinWorkerFactory(name) {
            @Override
            protected ExtendedForkJoinWorkerThread createThread(ForkJoinPool pool) {
                return new FiberWorkerThread(pool);
            }
        }, exceptionHandler, true);
        pool.setMonitor(createForkJoinPoolMonitor(name, pool, monitorType));
        return pool;
    }

    private static FibersMonitor createFibersMonitor(String name, FiberScheduler scheduler, MonitorType monitorType, boolean detailedInfo) {
        switch (monitorType) {
            case JMX:
                return new JMXFibersMonitor(name, scheduler, detailedInfo);
            case METRICS:
                return new MetricsFibersMonitor(name, scheduler);
            case NONE:
                return NOOP_FIBERS_MONITOR;
            default:
                throw new RuntimeException("Unsupported monitor type: " + monitorType);
        }
    }

    static ForkJoinPoolMonitor createForkJoinPoolMonitor(String name, ForkJoinPool fjPool, MonitorType monitorType) {
        if (monitorType == null)
            return null;
        switch (monitorType) {
            case JMX:
                return new JMXForkJoinPoolMonitor(name, fjPool);
            case METRICS:
                return new MetricsForkJoinPoolMonitor(name, fjPool);
            case NONE:
                return null;
            default:
                throw new RuntimeException("Unsupported monitor type: " + monitorType);
        }
    }

    private FiberTimedScheduler createTimer(ForkJoinPool fjPool, FibersMonitor monitor) {
        if (fjPool instanceof MonitoredForkJoinPool)
            return new FiberTimedScheduler(this,
                    new ThreadFactoryBuilder().setDaemon(true).setNameFormat("FiberTimedScheduler-" + ((MonitoredForkJoinPool) fjPool).getName()).build(),
                    monitor);
        else
            return new FiberTimedScheduler(this);
    }

    public ForkJoinPool getForkJoinPool() {
        return fjPool;
    }

    FiberTimedScheduler getTimer() {
        return timer;
    }

    FibersMonitor getFibersMonitor() {
        return fibersMonitor;
    }

    int getTimedQueueLength() {
        return timer.getQueueLength();
    }
    static boolean isFiberThread(Thread t) {
        return t instanceof FiberWorkerThread;
    }

    private class FiberWorkerThread extends ExtendedForkJoinWorkerThread {
        public FiberWorkerThread(ForkJoinPool pool) {
            super(pool);
        }

        @Override
        protected void onStart() {
            super.onStart();
            activeThreads.add(this);
        }

        @Override
        protected void onTermination(Throwable exception) {
            super.onTermination(exception);
            activeThreads.remove(this);
        }
    }

    Map<Thread, Fiber> getRunningFibers() {
        Map<Thread, Fiber> fibers = new HashMap<>(activeThreads.size() + 2);
        for (FiberWorkerThread t : activeThreads)
            fibers.put(t, Fiber.getTargetFiber(t));
        return fibers;
    }
}
