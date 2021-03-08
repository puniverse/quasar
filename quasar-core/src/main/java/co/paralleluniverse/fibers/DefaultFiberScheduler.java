/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.common.monitoring.MonitorType;
import java.lang.Thread.UncaughtExceptionHandler;

/**
 * The default {@link FiberScheduler} used to schedule fibers that do not specify a particular scheduler.
 * The properties of the default scheduler can be set with system properties:
 * <ul>
 * <li>{@code "co.paralleluniverse.fibers.DefaultFiberPool.parallelism"} - the number of threads in the default scheduler. By default, set equal to the number of available cores.</li>
 * <li>{@code "co.paralleluniverse.fibers.DefaultFiberPool.exceptionHandler"} - the name of the class to be used as the {@link UncaughtExceptionHandler UncaughtExceptionHandler}
 * (an instance is constructed using a public default constructor)</li>
 * <li>{@code "co.paralleluniverse.fibers.DefaultFiberPool.monitor"} - the {@link MonitorType monitor type} used to monitor the underlying {@code ForkJoinPool}.
 * May be {@code "JMX"} (the defualt), {@code "METRICS"}, or {@code "NONE"}.</li>
 * <li>{@code "co.paralleluniverse.fibers.DefaultFiberPool.detailedFiberInfo"} - whether the fibers monitor collects detailed information about running fibers.
 * May be {@code "true"} or {@code "false"} (the default)</li>
 * </ul>
 *
 * @author pron
 */
public class DefaultFiberScheduler {
    private static final String PROPERTY_PARALLELISM = "co.paralleluniverse.fibers.DefaultFiberPool.parallelism";
    private static final String PROPERTY_EXCEPTION_HANDLER = "co.paralleluniverse.fibers.DefaultFiberPool.exceptionHandler";
    private static final String PROPERTY_MONITOR_TYPE = "co.paralleluniverse.fibers.DefaultFiberPool.monitor";
    private static final String PROPERTY_DETAILED_FIBER_INFO = "co.paralleluniverse.fibers.DefaultFiberPool.detailedFiberInfo";
    private static final int MAX_CAP = 0x7fff;  // max #workers - 1
    private static final FiberScheduler instance;

    static {
        // defaults
        final String name = "default-fiber-pool";
        int par = 0;
        UncaughtExceptionHandler handler = null;
        // ForkJoinPool.ForkJoinWorkerThreadFactory fac = new NamingForkJoinWorkerFactory(name);
        MonitorType monitorType = null;
        boolean detailedFiberInfo = false;

        // get overrides
        try {
            String pp = System.getProperty(PROPERTY_PARALLELISM);
            String hp = System.getProperty(PROPERTY_EXCEPTION_HANDLER);
//            String fp = System.getProperty(PROPERTY_THREAD_FACTORY);
//            if (fp != null)
//                fac = ((ForkJoinPool.ForkJoinWorkerThreadFactory) ClassLoader.getSystemClassLoader().loadClass(fp).newInstance());
            if (hp != null)
                handler = ((UncaughtExceptionHandler) ClassLoader.getSystemClassLoader().loadClass(hp).newInstance());
            if (pp != null)
                par = Integer.parseInt(pp);
        } catch (Exception ignore) {
        }

        if (par <= 0)
            par = Runtime.getRuntime().availableProcessors();
        if (par > MAX_CAP)
            par = MAX_CAP;

        String mt = System.getProperty(PROPERTY_MONITOR_TYPE);
        if (mt != null)
            monitorType = MonitorType.valueOf(mt.toUpperCase());

        String dfis = System.getProperty(PROPERTY_DETAILED_FIBER_INFO);
        if (dfis != null)
            detailedFiberInfo = Boolean.parseBoolean(dfis);

        // build instance
        instance = new FiberForkJoinScheduler(name, par, handler, monitorType, detailedFiberInfo);
    }

    /**
     * Returns the The default {@link FiberScheduler} instance.
     *
     * @return the default {@link FiberScheduler} instance.
     */
    public static FiberScheduler getInstance() {
        return instance;
    }
}
