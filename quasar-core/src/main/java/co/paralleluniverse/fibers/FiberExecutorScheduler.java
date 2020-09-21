/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A fiber scheduler that uses a given {@link Executor} for scheduling.
 * 
 * The {@code Runnable} tasks passed to the {@link Executor} for execution,
 * implement the {@link FiberSchedulerTask} interface.
 *
 * @author pron
 */
public class FiberExecutorScheduler extends FiberScheduler implements Executor {
    private final Executor executor;
    private final FiberTimedScheduler timer;

    /**
     * Creates a new fiber scheduler.
     *
     * @param name         the scheuler's name. This name is used in naming the scheduler's threads.
     * @param executor     an {@link Executor} used to schedule the fibers;
     *                     may be {@code null} if the {@link #execute(Runnable)} method is overriden.
     * @param monitorType  the {@link MonitorType} type to use for the scheduler.
     * @param detailedInfo whether detailed information about the fibers is collected by the fibers monitor.
     */
    public FiberExecutorScheduler(String name, Executor executor, MonitorType monitorType, boolean detailedInfo) {
        super(name, monitorType, detailedInfo);
        this.executor = executor;
        this.timer = new FiberTimedScheduler(this,
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("FiberTimedScheduler-" + getName()).build(),
                getMonitor());
    }

    /**
     * Creates a new fiber scheduler with no monitor.
     *
     * @param name     the scheuler's name. This name is used in naming the scheduler's threads.
     * @param executor an {@link Executor} used to schedule the fibers;
     *                 may be {@code null} if the {@link #execute(Runnable)} method is overriden.
     */
    public FiberExecutorScheduler(String name, Executor executor) {
        this(name, executor, null, false);
    }

    public void shutdown() {
        this.timer.shutdown();
        super.shutdown();
    }

    @Override
    protected boolean isCurrentThreadInScheduler() {
        return false;
    }

    @Override
    protected int getQueueLength() {
        if (executor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) executor).getQueue().size();
        }
        return -1;
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    protected Map<Thread, Fiber<?>> getRunningFibers() {
        return null;
    }

    @Override
    public Executor getExecutor() {
        return executor != null ? executor : this;
    }

    @Override
    Future<Void> schedule(Fiber<?> fiber, Object blocker, long delay, TimeUnit unit) {
        return timer.schedule(fiber, blocker, delay, unit);
    }

    @Override
    <V> FiberTask<V> newFiberTask(Fiber<V> fiber) {
        return new RunnableFiberTask<>(fiber, this);
    }

    @Override
    int getTimedQueueLength() {
        return timer.getQueueLength();
    }

    @Override
    void setCurrentFiber(Fiber<?> target, Thread currentThread) {
        Fiber.setCurrentStrand(target);
    }

    @Override
    void setCurrentTarget(Object target, Thread currentThread) {
        Fiber.setCurrentStrand((Strand) target);
    }

    @Override
    Object getCurrentTarget(Thread currentThread) {
        return Fiber.getCurrentStrand();
    }
}
