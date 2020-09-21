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
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactory;
import co.paralleluniverse.strands.SuspendableCallable;
import com.google.common.collect.MapMaker;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A thread-pool based scheduler for fibers. Internally, this scheduler uses a {@code ForkJoinPool} to schedule fiber execution.
 *
 * @author pron
 */
public abstract class FiberScheduler implements FiberFactory, StrandFactory {
    static final FibersMonitor NOOP_FIBERS_MONITOR = new NoopFibersMonitor();
    private final String name;
    private final FibersMonitor fibersMonitor;
    final ConcurrentMap<SchedulerLocal, SchedulerLocal.Entry<?>> schedLocals = new MapMaker().weakKeys().makeMap();

    FiberScheduler(String name, MonitorType monitorType, boolean detailedInfo) {
        this.name = name;
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
    
    public void shutdown() {
    }

    public String getName() {
        return name;
    }

    protected FibersMonitor getMonitor() {
        return fibersMonitor;
    }

    @Override
    public <T> Fiber<T> newFiber(SuspendableCallable<T> target) {
        return new Fiber<T>(this, target);
    }

    @Override
    public Strand newStrand(final SuspendableCallable<?> target) {
        return newFiber(target);
    }
    
    abstract Future<Void> schedule(Fiber<?> fiber, Object blocker, long delay, TimeUnit unit);

    abstract Map<Thread, Fiber<?>> getRunningFibers();

    protected abstract int getQueueLength();

    abstract int getTimedQueueLength();

    protected abstract boolean isCurrentThreadInScheduler();

    void setCurrentFiber(Fiber<?> fiber, Thread currentThread) {
        Fiber.setCurrentStrand(fiber);
    }

    abstract void setCurrentTarget(Object target, Thread currentThread);

    abstract Object getCurrentTarget(Thread currentThread);

    abstract <V> FiberTask<V> newFiberTask(Fiber<V> fiber);
    
    public abstract Executor getExecutor();
}
