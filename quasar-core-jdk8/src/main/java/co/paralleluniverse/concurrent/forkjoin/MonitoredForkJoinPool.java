/*
 * Copyright (c) 2011-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.concurrent.forkjoin;

import co.paralleluniverse.common.monitoring.ForkJoinPoolMonitor;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author pron
 */
public class MonitoredForkJoinPool extends ForkJoinPool {
    private static final AtomicInteger ordinal = new AtomicInteger();
    private final String name;
    private ForkJoinPoolMonitor monitor;

    public MonitoredForkJoinPool(String name) {
        this.name = name != null ? name : ("ForkJoinPool-" + ordinal.incrementAndGet());
    }

    public MonitoredForkJoinPool(String name, int parallelism) {
        super(parallelism);
        this.name = name != null ? name : ("ForkJoinPool-" + ordinal.incrementAndGet());
    }

    public MonitoredForkJoinPool(String name, int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode) {
        super(parallelism, factory, handler, asyncMode);
        this.name = name != null ? name : ("ForkJoinPool-" + ordinal.incrementAndGet());
    }

    public MonitoredForkJoinPool() {
        this(null);
    }

    public MonitoredForkJoinPool(int parallelism) {
        this(null, parallelism);
    }

    public MonitoredForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode) {
        this(null, parallelism, factory, handler, asyncMode);
    }

    public String getName() {
        return name;
    }

    public void setMonitor(ForkJoinPoolMonitor monitor) {
        if (this.monitor != null)
            throw new IllegalStateException("Monitor already set");
        this.monitor = monitor;
    }

    public ForkJoinPoolMonitor getMonitor() {
        return monitor;
    }
}
