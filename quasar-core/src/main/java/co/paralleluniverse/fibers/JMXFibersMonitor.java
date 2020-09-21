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

import co.paralleluniverse.common.monitoring.Counter;
import co.paralleluniverse.common.monitoring.MonitoringServices;
import co.paralleluniverse.strands.Strand;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.StandardEmitterMBean;

/**
 * A JMX Mbean that monitors fibers runningin a single {@link FiberScheduler}.
 *
 * @author pron
 */
class JMXFibersMonitor extends StandardEmitterMBean implements FibersMonitor, NotificationListener, FibersMXBean {
    private final String mbeanName;
    private final FiberScheduler scheduler;
    private boolean registered;
    private long lastCollectTime;
    private final FibersDetailedMonitor details;
    private final Counter activeCount = new Counter();
    //private final Counter runnableCount = new Counter();
    private final Counter waitingCount = new Counter();
    private final Counter spuriousWakeupsCounter = new Counter();
    private final Counter timedWakeupsCounter = new Counter();
    private final Counter timedParkLatencyCounter = new Counter();
    private long spuriousWakeups;
    private long meanTimedWakeupLatency;
    private Map<Fiber<?>, StackTraceElement[]> problemFibers;
    private long notificationSequenceNumber = 1;

    public JMXFibersMonitor(String name, FiberScheduler scheduler, boolean detailedInfo) {
        super(FibersMXBean.class, true, new NotificationBroadcasterSupport());
        this.scheduler = scheduler;
        this.mbeanName = "co.paralleluniverse:type=Fibers,name=" + name;
        registerMBean();
        lastCollectTime = nanoTime();
        this.details = detailedInfo ? new FibersDetailedMonitor() : null;
    }

    @SuppressWarnings({"CallToPrintStackTrace", "CallToThreadDumpStack"})
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

    @SuppressWarnings({"CallToPrintStackTrace", "CallToThreadDumpStack"})
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
    public MBeanNotificationInfo[] getNotificationInfo() {
        MBeanNotificationInfo info = new MBeanNotificationInfo(
                new String[]{RunawayFiberNotification.NAME}, 
                RunawayFiberNotification.class.getName(), 
                "Runaway fiber detected");
        return new MBeanNotificationInfo[]{info};
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
        spuriousWakeups = spuriousWakeupsCounter.getAndReset();

        final long tw = timedWakeupsCounter.getAndReset();
        final long tpl = timedParkLatencyCounter.getAndReset();

        meanTimedWakeupLatency = tw != 0L ? tpl / tw : 0L;

        lastCollectTime = nanoTime();
    }

    private long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public void fiberStarted(Fiber<?> fiber) {
        activeCount.inc();
        if (details != null)
            details.fiberStarted(fiber);
    }

    @Override
    public void fiberTerminated(Fiber<?> fiber) {
        activeCount.dec();
        //runnableCount.dec();
        if (details != null)
            details.fiberTerminated(fiber);
    }

    @Override
    public void fiberSuspended() {
        //runnableCount.dec();
        waitingCount.inc();
    }

    @Override
    public void fiberResumed() {
        //runnableCount.inc();
        waitingCount.dec();
    }

    @Override
    public void spuriousWakeup() {
        spuriousWakeupsCounter.inc();
    }

    @Override
    public void timedParkLatency(long ns) {
        timedWakeupsCounter.inc();
        timedParkLatencyCounter.add(ns);
    }

    @Override
    public void setRunawayFibers(Collection<Fiber<?>> fs) {
        if (fs == null || fs.isEmpty())
            this.problemFibers = null;
        else {
            Map<Fiber<?>, StackTraceElement[]> map = new HashMap<>();
            for (Fiber<?> f : fs) {
                Thread t = f.getRunningThread();
                final String status;
                if (t == null)
                    status = "hogging the CPU or blocking a thread";
                else if (t.getState() == Thread.State.RUNNABLE)
                    status = "hogging the CPU (" + t + ")";
                else
                    status = "blocking a thread (" + t + ")";
                StackTraceElement[] st = f.getStackTrace();

                Map<Fiber<?>, StackTraceElement[]> pf = problemFibers;
                if (pf == null || !pf.containsKey(f)) {
                    Notification n = new RunawayFiberNotification(this, notificationSequenceNumber++, System.currentTimeMillis(),
                            "Runaway fiber " + f.getName() + " is " + status + ":\n" + Strand.toString(st));
                    sendNotification(n);
                }

                map.put(f, st);
            }
            this.problemFibers = map;
        }
    }

    @Override
    public Map<String, String> getRunawayFibers() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<Fiber<?>, StackTraceElement[]> e : problemFibers.entrySet())
            map.put(e.getKey().toString(), Strand.toString(e.getValue()));
        return map;
    }

    @Override
    public int getNumActiveFibers() {
        return (int)activeCount.get();
    }

    @Override
    public int getNumRunnableFibers() {
        return getNumActiveFibers() - getNumWaitingFibers();
        //return runnableCount.intValue();
    }

    @Override
    public int getNumWaitingFibers() {
        return (int)waitingCount.get();
    }

    @Override
    public int getTimedQueueLength() {
        return scheduler.getTimedQueueLength();
    }

    @Override
    public long getSpuriousWakeups() {
        return spuriousWakeups;
    }

    @Override
    public long getMeanTimedWakeupLatency() {
        return meanTimedWakeupLatency;
    }

    @Override
    public long[] getAllFiberIds() {
        if (details == null)
            return null;
        return details.getAllFiberIds();
    }

    @Override
    public FiberInfo getFiberInfo(long id, boolean stack) {
        if (details == null)
            return null;
        return details.getFiberInfo(id, stack);
    }

    @Override
    public FiberInfo[] getFiberInfo(long[] ids, boolean stack) {
        if (details == null)
            return null;
        return details.getFiberInfo(ids, stack);
    }

    private static class RunawayFiberNotification extends Notification {
        static final String NAME = "co.paralleluniverse.fibers.runawayfiber";

        public RunawayFiberNotification(String type, Object source, long sequenceNumber, String message) {
            super(NAME, source, sequenceNumber, message);
        }

        public RunawayFiberNotification(Object source, long sequenceNumber, long timeStamp, String message) {
            super(NAME, source, sequenceNumber, timeStamp, message);
        }
    }
}
