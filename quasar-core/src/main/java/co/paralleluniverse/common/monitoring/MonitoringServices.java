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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.timer.Timer;

/**
 *
 * @author pron
 */
public final class MonitoringServices implements MonitoringServicesMXBean {
    public static final MonitoringServices instance = new MonitoringServices();

    public static MonitoringServices getInstance() {
        return instance;
    }
    private final Timer timer = new Timer();
    private int perfTimerPeriod = 5000;
    private int structuralTimerPeriod = 30000;
    private boolean perfTimerStarted;
    private boolean structuralTimerStarted;
    private int perfTimerListeners;
    private int structuralTimerListeners;

    private MonitoringServices() {
        registerMBean(true);
        perfTimerListeners = 0;
        structuralTimerListeners = 0;

        killTimerOnExit();

        startPerformanceUpdates();
    }

    private synchronized void manageTimer() {
        if (!timer.isActive()) {
            if ((perfTimerStarted && perfTimerListeners > 0) || (structuralTimerStarted && structuralTimerListeners > 0))
                timer.start();
        } else {
            if ((!perfTimerStarted || perfTimerListeners == 0) && (!structuralTimerStarted || structuralTimerListeners == 0))
                timer.stop();
        }
    }

    private void registerMBean(boolean retry) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName mxbeanName = new ObjectName("co.paralleluniverse:name=MonitoringServices");
            mbs.registerMBean(this, mxbeanName);
        } catch (InstanceAlreadyExistsException ex) {
            if (retry) {
                try {
                    ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName("co.paralleluniverse:name=MonitoringServices"));
                } catch (InstanceNotFoundException | MalformedObjectNameException | MBeanRegistrationException ex2) {
                    throw new AssertionError(ex);
                }
                registerMBean(false);
            } else {
                throw new RuntimeException(ex);
            }
        } catch (MBeanRegistrationException ex) {
            ex.printStackTrace();
        } catch (NotCompliantMBeanException ex) {
            throw new AssertionError(ex);
        } catch (MalformedObjectNameException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public synchronized int getPerformanceTimerPeriod() {
        return perfTimerPeriod;
    }

    @Override
    public synchronized void setPerformanceTimerPeriod(int perfTimerPeriod) {
        if (perfTimerPeriod != this.perfTimerPeriod) {
            this.perfTimerPeriod = perfTimerPeriod;
            if (perfTimerStarted) {
                stopPerformanceUpdates();
                startPerformanceUpdates();
            }
        }
    }

    @Override
    public synchronized boolean isPerformanceUpdates() {
        return perfTimerStarted;
    }

    @Override
    public synchronized void setPerformanceUpdates(boolean value) {
        if (value == perfTimerStarted)
            return;
        if (!perfTimerStarted)
            startPerformanceUpdates();
        else
            stopPerformanceUpdates();
    }

    @Override
    public synchronized void startPerformanceUpdates() {
        if (!perfTimerStarted) {
            timer.addNotification("perfTimer", null, null, new Date(System.currentTimeMillis()), perfTimerPeriod);
            this.perfTimerStarted = true;
        }
        manageTimer();
    }

    @Override
    public synchronized void stopPerformanceUpdates() {
        if (perfTimerStarted) {
            try {
                timer.removeNotifications("perfTimer");
                this.perfTimerStarted = false;
            } catch (InstanceNotFoundException ex) {
            }
        }
        manageTimer();
    }

    public synchronized void addPerfNotificationListener(NotificationListener listener, Object handback) {
        timer.addNotificationListener(listener, new NotificationFilter() {
            @Override
            public boolean isNotificationEnabled(Notification notification) {
                return "perfTimer".equals(notification.getType());
            }
        }, handback);
        perfTimerListeners++;
        manageTimer();
    }

    public synchronized void removePerfNotificationListener(NotificationListener listener) {
        try {
            timer.removeNotificationListener(listener);
            perfTimerListeners--;
            manageTimer();
        } catch (ListenerNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public synchronized boolean isStructuralUpdates() {
        return structuralTimerStarted;
    }

    @Override
    public synchronized void setStructuralUpdates(boolean value) {
        if (value == structuralTimerStarted)
            return;
        if (!structuralTimerStarted)
            startStructuralUpdates();
        else
            stopStructuralUpdates();
    }

    @Override
    public synchronized void startStructuralUpdates() {
        if (!structuralTimerStarted) {
            timer.addNotification("structTimer", null, null, new Date(System.currentTimeMillis()), structuralTimerPeriod);
            this.structuralTimerStarted = true;
        }
        manageTimer();
    }

    @Override
    public synchronized void stopStructuralUpdates() {
        if (structuralTimerStarted) {
            try {
                timer.removeNotifications("structTimer");
                this.structuralTimerStarted = false;
            } catch (InstanceNotFoundException ex) {
            }
        }
        manageTimer();
    }

    @Override
    public synchronized int getStructuraltimerPeriod() {
        return structuralTimerPeriod;
    }

    @Override
    public synchronized void setStructuraltimerPeriod(int structuralTimerPeriod) {
        if (structuralTimerPeriod != this.structuralTimerPeriod) {
            this.structuralTimerPeriod = structuralTimerPeriod;
            if (structuralTimerStarted) {
                stopStructuralUpdates();
                startStructuralUpdates();
            }
        }
    }

    public synchronized void addStructuralNotificationListener(NotificationListener listener, Object handback) {
        timer.addNotificationListener(listener, new NotificationFilter() {
            @Override
            public boolean isNotificationEnabled(Notification notification) {
                return "structTimer".equals(notification.getType());
            }
        }, handback);
        structuralTimerListeners++;
        manageTimer();
    }

    public synchronized void removeStructuralNotificationListener(NotificationListener listener) {
        try {
            timer.removeNotificationListener(listener);
            structuralTimerListeners--;
            manageTimer();
        } catch (ListenerNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private void killTimerOnExit() {
        // unfortunately, there's no way to make the timer thread a daemon,
        // wo kill timer thread if it's the last non-daemon thread.
        timer.addNotification("isLastThreadTest", null, null, new Date(System.currentTimeMillis()), 1500);
        timer.addNotificationListener(new NotificationListener() {
            @Override
            public void handleNotification(Notification notification, Object handback) {
                final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
                int tCount = mbean.getThreadCount();
                int dCount = mbean.getDaemonThreadCount();

                if (tCount - dCount <= 2) {
                    boolean stop = true;
                    Thread[] ts;

                    for (;;) {
                        ts = new Thread[mbean.getThreadCount() + 3];
                        if (Thread.enumerate(ts) < ts.length)
                            break;
                    }
                    for (Thread t : ts) {
                        if(t == null)
                            break;
                        if (t.isDaemon() || t == Thread.currentThread())
                            continue;
                        if (!t.getName().equals("DestroyJavaVM"))
                            stop = false;
                    }

                    if (stop)
                        timer.stop();
                }
            }
        }, new NotificationFilter() {
            @Override
            public boolean isNotificationEnabled(Notification notification) {
                return "isLastThreadTest".equals(notification.getType());
            }
        }, null);
    }
}
