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
package co.paralleluniverse.common.monitoring;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Histogram;
import java.util.Map;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public class MetricsForkJoinPoolMonitor extends JMXForkJoinPoolMonitor {
    private final Histogram runsPerTask;
    
    public MetricsForkJoinPoolMonitor(String name, ForkJoinPool fjPool, Map<?, Integer> highContentionObjects) {
        super(name, fjPool, highContentionObjects);
        
        this.runsPerTask = Metrics.newHistogram(MetricsForkJoinPoolMonitor.class, "runsPerTask", name, true);
    }

    @Override
    public void doneTask(int runs) {
        runsPerTask.update(runs);
    }
}
