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
import jsr166e.ForkJoinPool;
import jsr166e.LongAdder;

/**
 * A JMX Mbean that monitors fibers runningin a single {@link FiberScheduler}.
 *
 * @author pron
 */
class JMXFibersMonitor extends StandardEmitterMBean implements FibersMonitor, NotificationListener, FibersMXBean {
    private final String mbeanName;
    private boolean registered;
    private long lastCollectTime;
    private final FibersDetailedMonitor details;
    private final LongAdder activeCount = new LongAdder();
    //private final LongAdder runnableCount = new LongAdder();
    private final LongAdder waitingCount = new LongAdder();
    private final LongAdder spuriousWakeupsCounter = new LongAdder();
    private final LongAdder timedWakeupsCounter = new LongAdder();
    private final LongAdder timedParkLatencyCounter = new LongAdder();
    private long spuriousWakeups;
    private long meanTimedWakeupLatency;
    private Map<Fiber, StackTraceElement[]> problemFibers;
    private long notificationSequenceNumber = 1;

    public JMXFibersMonitor(String name, ForkJoinPool fjPool, boolean detailedInfo) {
        super(FibersMXBean.class, true, new NotificationBroadcasterSupport());
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
        String[] types = new String[]{
            RunawayFiberNotification.NAME
        };
        String notifName = RunawayFiberNotification.class.getName();
        String description = "Runaway fiber detected";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, notifName, description);
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
    public void fiberStarted(Fiber fiber) {
        activeCount.increment();
        if (details != null)
            details.fiberStarted(fiber);
    }

    @Override
    public void fiberTerminated(Fiber fiber) {
        activeCount.decrement();
        //runnableCount.decrement();
        if (details != null)
            details.fiberTerminated(fiber);
    }

    @Override
    public void fiberSuspended() {
        //runnableCount.decrement();
        waitingCount.increment();
    }

    @Override
    public void fiberResumed() {
        //runnableCount.increment();
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
    public void setRunawayFibers(Collection<Fiber> fs) {
        if (fs.isEmpty())
            this.problemFibers = null;
        else {
            Map<Fiber, StackTraceElement[]> map = new HashMap<>();
            for (Fiber f : fs) {
                Thread t = f.getRunningThread();
                final String status;
                if (t == null)
                    status = "hogging the CPU or blocking a thread";
                else if (t.getState() == Thread.State.RUNNABLE)
                    status = "hogging the CPU (" + t + ")";
                else
                    status = "blocking a thread (" + t + ")";
                StackTraceElement[] st = f.getStackTrace();

                if (!problemFibers.containsKey(f)) {
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
        for (Map.Entry<Fiber, StackTraceElement[]> e : problemFibers.entrySet())
            map.put(e.getKey().toString(), Strand.toString(e.getValue()));
        return map;
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
