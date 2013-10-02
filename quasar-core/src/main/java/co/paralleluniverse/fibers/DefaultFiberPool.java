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
import co.paralleluniverse.concurrent.forkjoin.MonitoredForkJoinPool;
import co.paralleluniverse.concurrent.forkjoin.NamingForkJoinWorkerFactory;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public class DefaultFiberPool {
    private static final int MAX_CAP = 0x7fff;  // max #workers - 1
    private static final MonitoredForkJoinPool instance;

    static {
        int par = 0;
        Thread.UncaughtExceptionHandler handler = null;
        ForkJoinPool.ForkJoinWorkerThreadFactory fac = new NamingForkJoinWorkerFactory("default-fiber-pool");
        String monitorType = "JMX";

        try {
            String pp = System.getProperty("co.paralleluniverse.fibers.DefaultFiberPool.parallelism");
            String hp = System.getProperty("co.paralleluniverse.fibers.DefaultFiberPool.exceptionHandler");
            String fp = System.getProperty("co.paralleluniverse.fibers.DefaultFiberPool.threadFactory");
            if (fp != null)
                fac = ((ForkJoinPool.ForkJoinWorkerThreadFactory) ClassLoader.getSystemClassLoader().loadClass(fp).newInstance());
            if (hp != null)
                handler = ((Thread.UncaughtExceptionHandler) ClassLoader.getSystemClassLoader().loadClass(hp).newInstance());
            if (pp != null)
                par = Integer.parseInt(pp);
        } catch (Exception ignore) {
        }

        if (par <= 0)
            par = Runtime.getRuntime().availableProcessors();
        if (par > MAX_CAP)
            par = MAX_CAP;

        String mt = System.getProperty("co.paralleluniverse.fibers.DefaultFiberPool.monitor");
        if (mt != null)
            monitorType = mt.toUpperCase();

        instance = new MonitoredForkJoinPool("default-fiber-pool", par, fac, handler, true);
        
        final ForkJoinPoolMonitor fjpMonitor;
        final FibersMonitor fibersMonitor;
        switch (monitorType) {
            case "JMX":
                fjpMonitor = new JMXForkJoinPoolMonitor(instance.getName(), instance);
                fibersMonitor = new JMXFibersMonitor(instance.getName(), instance);
                break;
            case "METRICS":
                fjpMonitor = new MetricsForkJoinPoolMonitor(instance.getName(), instance);
                fibersMonitor = new MetricsFibersMonitor(instance.getName(), instance);
                break;
            case "NONE":
                fjpMonitor = null;
                fibersMonitor = new NoopFibersMonitor();
                break;
            default:
                throw new RuntimeException("Unsupported monitor type: " + monitorType);
        }

        instance.setMonitor(fjpMonitor);
        instance.setFibersMonitor(fibersMonitor);
    }

    public static ForkJoinPool getInstance() {
        return instance;
    }
}
