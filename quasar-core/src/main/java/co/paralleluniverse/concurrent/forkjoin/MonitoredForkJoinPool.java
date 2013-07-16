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
package co.paralleluniverse.concurrent.forkjoin;

import co.paralleluniverse.common.monitoring.ForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.ForkJoinPoolMonitorFactory;
import co.paralleluniverse.common.monitoring.JMXForkJoinPoolMonitor;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicInteger;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public class MonitoredForkJoinPool extends ForkJoinPool {
    private static ForkJoinPoolMonitorFactory defaultForkJoinPoolMonitorFactory = new ForkJoinPoolMonitorFactory() {
        @Override
        public ForkJoinPoolMonitor newMonitor(String name, ForkJoinPool fjPool) {
            return new JMXForkJoinPoolMonitor(name, fjPool);
        }
    };
    private static final AtomicInteger ordinal = new AtomicInteger();
    private final String name;
    private ForkJoinPoolMonitor monitor;

    public MonitoredForkJoinPool(String name, ForkJoinPoolMonitorFactory monitorFactory) {
        this.name = name != null ? name : ("ForkJoinPool-" + ordinal.incrementAndGet());
        this.monitor = monitorFactory.newMonitor(this.name, this);
    }

    public MonitoredForkJoinPool(String name, ForkJoinPoolMonitorFactory monitorFactory, int parallelism) {
        super(parallelism);
        this.name = name != null ? name : ("ForkJoinPool-" + ordinal.incrementAndGet());
        this.monitor = monitorFactory.newMonitor(this.name, this);
    }

    public MonitoredForkJoinPool(String name, ForkJoinPoolMonitorFactory monitorFactory, int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode) {
        super(parallelism, factory, handler, asyncMode);
        this.name = name != null ? name : ("ForkJoinPool-" + ordinal.incrementAndGet());
        this.monitor = monitorFactory.newMonitor(this.name, this);
    }

    public MonitoredForkJoinPool(String name) {
        this(name, defaultForkJoinPoolMonitorFactory);
    }

    public MonitoredForkJoinPool(String name, int parallelism) {
        this(name, defaultForkJoinPoolMonitorFactory, parallelism);
    }

    public MonitoredForkJoinPool(String name, int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode) {
        this(name, defaultForkJoinPoolMonitorFactory, parallelism, factory, handler, asyncMode);
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
        if(this.monitor != null)
            this.monitor.unregister();
        this.monitor = monitor;
    }
    
    public ForkJoinPoolMonitor getMonitor() {
        return monitor;
    }
}
