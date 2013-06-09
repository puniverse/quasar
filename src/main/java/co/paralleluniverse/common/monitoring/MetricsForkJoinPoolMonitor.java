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

import com.codahale.metrics.Histogram;
import static com.codahale.metrics.MetricRegistry.name;
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
        
        this.runsPerTask = Metrics.histogram(name(MetricsForkJoinPoolMonitor.class, "runsPerTask", name));
    }

    @Override
    public void doneTask(int runs) {
        runsPerTask.update(runs);
    }
}
