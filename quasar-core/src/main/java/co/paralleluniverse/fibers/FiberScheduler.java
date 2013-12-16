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

import co.paralleluniverse.common.monitoring.MonitorType;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A thread-pool based scheduler for fibers. Internally, this scheduler uses a {@code ForkJoinPool} to schedule fiber execution.
 *
 * @author pron
 */
public abstract class FiberScheduler {
    static final FibersMonitor NOOP_FIBERS_MONITOR = new NoopFibersMonitor();
    private final FibersMonitor fibersMonitor;

    FiberScheduler(String name, MonitorType monitorType, boolean detailedInfo) {
        fibersMonitor = createFibersMonitor(name, this, monitorType, detailedInfo);
    }

    private static FibersMonitor createFibersMonitor(String name, FiberScheduler scheduler, MonitorType monitorType, boolean detailedInfo) {
        if (monitorType == null)
            monitorType = MonitorType.NONE;
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

    protected FibersMonitor getMonitor() {
        return fibersMonitor;
    }

    public abstract Future<Void> schedule(Fiber<?> fiber, Object blocker, long delay, TimeUnit unit);

    protected abstract Map<Thread, Fiber> getRunningFibers();

    protected abstract int getQueueLength();

    protected abstract int getTimedQueueLength();

    protected abstract boolean isCurrentThreadInScheduler();

    protected void setCurrentFiber(Fiber fiber, Thread currentThread) {
        Fiber.currentFiber.set(fiber);
    }

    protected abstract void setCurrentTarget(Object target, Thread currentThread);

    protected abstract Object getCurrentTarget(Thread currentThread);

    protected abstract <V> FiberTask<V> newFiberTask(Fiber<V> fiber);
}
