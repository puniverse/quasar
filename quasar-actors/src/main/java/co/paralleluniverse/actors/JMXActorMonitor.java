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
package co.paralleluniverse.actors;

import co.paralleluniverse.common.monitoring.Counter;
import co.paralleluniverse.common.monitoring.MonitoringServices;
import co.paralleluniverse.common.util.Objects;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.management.AttributeChangeNotification;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.StandardEmitterMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class JMXActorMonitor extends StandardEmitterMBean implements ActorMonitor, ActorMXBean, NotificationListener, NotificationEmitter {
    private static final Logger LOG = LoggerFactory.getLogger(JMXActorMonitor.class);
    /*
     * For the time being, we're not worried about data races. Messages counters are all updated by the actor, so there's no problem there.
     * For the JMX thread to see the messages counter, it should really be volatile, but as an approximation, we keep it a regular int.
     */
    private WeakReference<ActorRef> actor;
    private final String name;
    private boolean registered;
    private long lastCollectTime;
    private int notificationSequenceNumber;
    //
    private int messageCounter;
    private int skippedMessageCounter;
    private final Counter restartCounter = new Counter();
    private final Queue<String> deathCauses = new ConcurrentLinkedQueue<>();
    // These hold counter values for the previous window
    private volatile long messages;
    private volatile long skippedMessages;
    //

    public JMXActorMonitor(String name) {
        super(ActorMXBean.class, true, new NotificationBroadcasterSupport());
        this.name = beanName(name);
        LOG.info("Starting monitor {}: {}", name, this.name);
        lastCollectTime = nanoTime();
        refresh();
        registerMBean();
    }

    private static String beanName(String name) {
        return "co.paralleluniverse:type=quasar,monitor=actor,name=" + name;
    }

    @Override
    public void setActor(ActorRef actor) {
        if (actor == null && this.actor == null)
            return;
        LOG.info("Setting actor {} for monitor {}", actor, name);
        reset();
        this.actor = (actor != null ? new WeakReference<ActorRef>(actor) : null);
    }

    @Override
    public void shutdown() {
        LOG.info("Stopping monitor {}", name);
        unregisterMBean();
        this.actor = null;
    }

    private void registerMBean() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName mxbeanName = new ObjectName(name);

            if (mbs.isRegistered(mxbeanName)) {
                try {
                    LOG.info("MBean named {} is already registered. Unregistering it.", name);
                    mbs.unregisterMBean(mxbeanName);
                } catch (InstanceNotFoundException e) {
                }
            }
            mbs.registerMBean(this, mxbeanName);

            MonitoringServices.getInstance().addPerfNotificationListener(this, name);
            this.registered = true;
        } catch (InstanceAlreadyExistsException ex) {
            throw new RuntimeException(ex);
        } catch (MBeanRegistrationException ex) {
            ex.printStackTrace();
        } catch (NotCompliantMBeanException | MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    public void unregisterMBean() {
        try {
            if (registered) {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(name));
                MonitoringServices.getInstance().removePerfNotificationListener(this);
            }
            this.registered = false;
        } catch (InstanceNotFoundException | MBeanRegistrationException | MalformedObjectNameException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[]{
            AttributeChangeNotification.ATTRIBUTE_CHANGE
        };
        String notifName = AttributeChangeNotification.class.getName();
        String description = "An attribute of this MBean has changed";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, notifName, description);
        return new MBeanNotificationInfo[]{info};
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        if ("perfTimer".equals(notification.getType())) {
            assert Objects.equal(handback, name);
            refresh();
        }
    }

    @Override
    public void refresh() {
        if (registered) {
            final WeakReference<ActorRef> a1 = actor;
            if (a1 != null && a1.get() == null)
                unregisterMBean();
            else
                collect(nanoTime() - lastCollectTime);

            reset();
        }
    }

    protected void collect(long intervalNanos) {
        messages += messageCounter;
    }

    protected void reset() {
        messageCounter = 0;

        lastCollectTime = nanoTime();
    }

    private long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public void addDeath(Object cause) {
        if (cause == null)
            cause = "normal";
        while (deathCauses.size() > 20)
            deathCauses.poll();
        deathCauses.add(cause.toString());
    }

    @Override
    public void addRestart() {
        restartCounter.inc();
    }

    @Override
    public void addMessage() {
        messageCounter++;
    }

    @Override
    public void skippedMessage() {
        skippedMessageCounter++;
    }

    @Override
    public void resetSkippedMessages() {
        skippedMessageCounter = 0;
    }

    /////////
    @Override
    public int getQueueLength() {
        if (this.actor == null)
            return 0;
        final ActorRef a = this.actor.get();
        if (a == null)
            return 0;
        return LocalActor.getQueueLength(a);
    }

    @Override
    public long getReceivedMessages() {
        return messages;
    }

    @Override
    public int getRestarts() {
        return (int) restartCounter.get();
    }

    @Override
    public String[] getLastDeathCauses() {
        return deathCauses.toArray(new String[0]);
    }

    @Override
    public String[] mailbox() {
        final WeakReference<ActorRef> a1 = actor;
        final ActorRef a = a1 != null ? a1.get() : null;
        if (a == null) {
            unregisterMBean();
            return null;
        }

        List<Object> list = LocalActor.getMailboxSnapshot(a);
        String[] ms = new String[list.size()];
        int i = 0;
        for (Object m : list)
            ms[i++] = m.toString();
        return ms;
    }

    @Override
    public String stackTrace() {
        final WeakReference<ActorRef> a1 = actor;
        final ActorRef a = a1 != null ? a1.get() : null;
        if (a == null) {
            unregisterMBean();
            return null;
        }

        final StackTraceElement[] stackTrace = LocalActor.getStackTrace(a);
        final StringBuilder sb = new StringBuilder();
        for (StackTraceElement ste : stackTrace)
            sb.append(ste).append('\n');
        return sb.toString();
    }
}
