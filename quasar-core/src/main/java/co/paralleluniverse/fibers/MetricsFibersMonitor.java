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

import co.paralleluniverse.common.monitoring.Metrics;
import co.paralleluniverse.common.monitoring.MetricsForkJoinPoolMonitor;
import com.codahale.metrics.Counter;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
class MetricsFibersMonitor extends MetricsForkJoinPoolMonitor implements FibersMonitor {
    private final Counter activeCount;
    //private final Counter runnableCount;
    private final Counter waitingCount;

    public MetricsFibersMonitor(String name, ForkJoinPool fjPool) {
        super(name, fjPool);
        this.activeCount = Metrics.counter(metric(name, "numActiveFibers"));
        this.waitingCount = Metrics.counter(metric(name, "numWaitingFibers"));
    }

    @Override
    public void fiberStarted() {
        activeCount.inc();
    }

    @Override
    public void fiberTerminated() {
        activeCount.dec();
        //runnableCount.dec();
    }

    @Override
    public void fiberSuspended() {
        //runnableCount.dec();
        waitingCount.inc();
    }

    @Override
    public void fiberSubmitted(boolean start) {
        //runnableCount.inc();
        if (start)
            activeCount.inc();
        else
            waitingCount.dec();
    }
}
