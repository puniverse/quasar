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
package co.paralleluniverse.common.monitoring;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;

/**
 *
 * @author pron
 */
public class Metrics {
    private static final MetricRegistry metrics = new MetricRegistry();
    private static final JmxReporter reporter;
    
    static {
        reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();
    }

    public static MetricRegistry registry() {
        return metrics;
    }
    
    public static <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        return metrics.register(name, metric);
    }

    public static void registerAll(MetricSet ms) throws IllegalArgumentException {
        metrics.registerAll(ms);
    }

    public static Counter counter(String name) {
        return metrics.counter(name);
    }

    public static Histogram histogram(String name) {
        return metrics.histogram(name);
    }

    public static Meter meter(String name) {
        return metrics.meter(name);
    }

    public static Timer timer(String name) {
        return metrics.timer(name);
    }

    public static boolean remove(String name) {
        return metrics.remove(name);
    }
}
