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
import co.paralleluniverse.concurrent.forkjoin.MonitoredForkJoinPool;
import co.paralleluniverse.concurrent.forkjoin.NamingForkJoinWorkerFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public class FiberScheduler {
    private final ForkJoinPool fjPool;
    private final FiberTimedScheduler timer;
    private final FibersMonitor fibersMonitor;

    public FiberScheduler(ForkJoinPool fjPool, FiberTimedScheduler timeService) {
        if(!fjPool.getAsyncMode())
            throw new IllegalArgumentException("ForkJoinPool is not async");
        this.fjPool = fjPool;
        
        
        if(fjPool instanceof MonitoredForkJoinPool) {
            final MonitoredForkJoinPool pool = (MonitoredForkJoinPool)fjPool;
            String name = pool.getName();
            if(pool.getMonitor() != null) {
                if(pool.getMonitor() instanceof JMXForkJoinPoolMonitor)
                    this.fibersMonitor = createFibersMonitor(name, fjPool, MonitorType.JMX);
                else if(pool.getMonitor() instanceof MetricsForkJoinPoolMonitor)
                    this.fibersMonitor = createFibersMonitor(name, fjPool, MonitorType.METRICS);
                else
                    throw new RuntimeException("Unrecognized ForkJoinPoolMonitor type: " + pool.getMonitor().getClass().getName());
            } else
                this.fibersMonitor = new NoopFibersMonitor();
        } else
            this.fibersMonitor = new NoopFibersMonitor();
        
        this.timer = timeService != null ? timeService : createTimer(fjPool, fibersMonitor);
    }

    public FiberScheduler(ForkJoinPool fjPool) {
        this(fjPool, null);
    }

    public FiberScheduler(String name, int parallelism, MonitorType monitorType) {
        this(createForkJoinPool(name, parallelism, monitorType));
    }

    private static ForkJoinPool createForkJoinPool(String name, int parallelism, MonitorType monitorType) {
        final MonitoredForkJoinPool fjPool = new MonitoredForkJoinPool(name, parallelism, new NamingForkJoinWorkerFactory(name), null, true);
        fjPool.setMonitor(createForkJoinPoolMonitor(name, fjPool, monitorType));
        return fjPool;
    }
    
    private static FibersMonitor createFibersMonitor(String name, ForkJoinPool fjPool, MonitorType monitorType) {
        switch (monitorType) {
            case JMX:
                return new JMXFibersMonitor(name, fjPool);
            case METRICS:
                return new MetricsFibersMonitor(name, fjPool);
            case NONE:
                return new NoopFibersMonitor();
            default:
                throw new RuntimeException("Unsupported monitor type: " + monitorType);
        }
    }

    private static ForkJoinPoolMonitor createForkJoinPoolMonitor(String name, ForkJoinPool fjPool, MonitorType monitorType) {
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

    private static FiberTimedScheduler createTimer(ForkJoinPool fjPool, FibersMonitor monitor) {
        if (fjPool instanceof MonitoredForkJoinPool)
            return new FiberTimedScheduler(fjPool,
                    new ThreadFactoryBuilder().setDaemon(true).setNameFormat(((MonitoredForkJoinPool) fjPool).getName()).build(),
                    monitor);
        else
            return new FiberTimedScheduler(fjPool);
    }

    public ForkJoinPool getFjPool() {
        return fjPool;
    }

    FiberTimedScheduler getTimer() {
        return timer;
    }

    FibersMonitor getFibersMonitor() {
        return fibersMonitor;
    }
}
