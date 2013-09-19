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

import co.paralleluniverse.common.monitoring.JMXForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.MonitoringServices;
import javax.management.Notification;
import javax.management.NotificationListener;
import jsr166e.ForkJoinPool;
import jsr166e.LongAdder;

/**
 *
 * @author pron
 */
class JMXFibersMonitor extends JMXForkJoinPoolMonitor implements FibersMonitor, NotificationListener, FibersForkJoinPoolMXBean {
    private final LongAdder activeCount = new LongAdder();
    //private final LongAdder runnableCount = new LongAdder();
    private final LongAdder waitingCount = new LongAdder();
    private long lastCollectTime;

    public JMXFibersMonitor(String name, ForkJoinPool fjPool) {
        super(name, fjPool);
        lastCollectTime = nanoTime();
    }

    @Override
    protected void registerMBean() {
        super.registerMBean();
        MonitoringServices.getInstance().addPerfNotificationListener(this, getMbeanName());
    }

    @Override
    public void unregister() {
        MonitoringServices.getInstance().removePerfNotificationListener(this);
        super.unregister();
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        if ("perfTimer".equals(notification.getType()))
            refresh();
    }

    @Override
    public void refresh() {
        collectAndResetCounters();
    }

    private void collectAndResetCounters() {
        if (isRegistered()) {
            fjPool();
            collect(nanoTime() - lastCollectTime);
            reset();
        }
    }

    protected void collect(long intervalNanos) {
    }

    protected void reset() {
        lastCollectTime = nanoTime();
    }

    private long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public void fiberStarted() {
        activeCount.increment();
    }

    @Override
    public void fiberTerminated() {
        activeCount.decrement();
        //runnableCount.decrement();
    }

    @Override
    public void fiberSuspended() {
        //runnableCount.decrement();
        waitingCount.increment();
    }

    @Override
    public void fiberSubmitted(boolean start) {
        //runnableCount.increment();
        if (start)
            activeCount.increment();
        else
            waitingCount.decrement();
    }

    @Override
    public int getNumActiveFibers() {
        return activeCount.intValue();
    }

    @Override
    public int getNumRunnableFibers() {
        return getNumActiveFibers() - getNumWaitingFibers();
        //return runnableCount.intValue();
    }

    @Override
    public int getNumWaitingFibers() {
        return waitingCount.intValue();
    }
}
