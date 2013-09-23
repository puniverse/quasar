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
import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import jsr166e.ForkJoinPool;
import jsr166e.LongAdder;

/**
 *
 * @author pron
 */
class JMXFibersMonitor implements FibersMonitor, NotificationListener, FibersMXBean {
    private final String mbeanName;
    private boolean registered;
    private final LongAdder activeCount = new LongAdder();
    //private final LongAdder runnableCount = new LongAdder();
    private final LongAdder waitingCount = new LongAdder();
    private final LongAdder spuriousWakeupsCounter = new LongAdder();
    private final LongAdder timedWakeupsCounter = new LongAdder();
    private final LongAdder timedParkLatencyCounter = new LongAdder();
    private long spuriousWakeups;
    private long meanTimedWakeupLatency;
    private long lastCollectTime;

    public JMXFibersMonitor(String name, ForkJoinPool fjPool) {
        this.mbeanName = "co.paralleluniverse:type=Fibers,name=" + name;
        registerMBean();
        lastCollectTime = nanoTime();
    }

    protected void registerMBean() {
        try {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final ObjectName mxbeanName = new ObjectName(mbeanName);
            mbs.registerMBean(this, mxbeanName);
            this.registered = true;
        } catch (InstanceAlreadyExistsException ex) {
            throw new RuntimeException(ex);
        } catch (MBeanRegistrationException ex) {
            ex.printStackTrace();
        } catch (NotCompliantMBeanException ex) {
            throw new AssertionError(ex);
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
        MonitoringServices.getInstance().addPerfNotificationListener(this, mbeanName);
    }

    @Override
    public void unregister() {
        try {
            if (registered) {
                MonitoringServices.getInstance().removePerfNotificationListener(this);
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(mbeanName));
            }
            this.registered = false;
        } catch (InstanceNotFoundException ex) {
            ex.printStackTrace();
        } catch (MBeanRegistrationException ex) {
            ex.printStackTrace();
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
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

    public boolean isRegistered() {
        return registered;
    }

    private void collectAndResetCounters() {
        if (isRegistered()) {
            collectAndResetCounters(nanoTime() - lastCollectTime);
        }
    }

    protected void collectAndResetCounters(long intervalNanos) {
        spuriousWakeups = spuriousWakeupsCounter.sumThenReset();

        final long tw = timedWakeupsCounter.sumThenReset();
        final long tpl = timedParkLatencyCounter.sumThenReset();

        meanTimedWakeupLatency = tw != 0L ? tpl / tw : 0L;

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
    public void spuriousWakeup() {
        spuriousWakeupsCounter.increment();
    }

    @Override
    public void timedParkLatency(long ns) {
        timedWakeupsCounter.increment();
        timedParkLatencyCounter.add(ns);
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

    @Override
    public long getSpuriousWakeups() {
        return spuriousWakeups;
    }

    @Override
    public long getMeanTimedWakeupLatency() {
        return meanTimedWakeupLatency;
    }
}
