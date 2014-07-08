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
import co.paralleluniverse.strands.Strand;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong.IteratorLong;

/**
 *
 * @author pron
 */
class JMXActorsMonitor implements NotificationListener, ActorsMXBean {
    private static final JMXActorsMonitor instance = new JMXActorsMonitor();

    public static JMXActorsMonitor getInstance() {
        return instance;
    }
    private final String mbeanName;
    private boolean registered;
    private final NonBlockingHashMapLong<ActorRef<?>> actors = new NonBlockingHashMapLong<ActorRef<?>>();
    private final NonBlockingHashMapLong<SmallActorMonitor> watchedActors = new NonBlockingHashMapLong<SmallActorMonitor>();
    private long lastCollectTime;
    private final Counter activeCount = new Counter();

    private JMXActorsMonitor() {
        this.mbeanName = "co.paralleluniverse:type=Actors";
        registerMBean();
        lastCollectTime = nanoTime();
    }

    @SuppressWarnings({"CallToPrintStackTrace", "CallToThreadDumpStack"})
    private void registerMBean() {
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
    private void unregister() {
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

        lastCollectTime = nanoTime();
    }

    private long nanoTime() {
        return System.nanoTime();
    }

    void actorStarted(ActorRef<?> actor) {
        activeCount.inc();
        actors.put(LocalActor.getStrand(actor).getId(), actor);
    }

    void actorTerminated(ActorRef<?> actor, Strand strand) {
        activeCount.dec();
        actors.remove(strand.getId());
    }

    @Override
    public int getNumActiveActors() {
        return (int)activeCount.get();
    }

    @Override
    public long[] getAllActorIds() {
        int size = actors.size();
        IteratorLong it = (IteratorLong) actors.keys();
        long[] ids = new long[size];
        int i = 0;
        while (it.hasNext() && i < size) {
            ids[i] = it.nextLong();
            i++;
        }
        if (i < size)
            return Arrays.copyOf(ids, i);
        else
            return ids; // might not include all nw fibers
    }

    @Override
    public String getStackTrace(long actorId) {
        ActorRef actor = actors.get(actorId);
        if (actor == null)
            return null;

        return getStackTrace(actor);
    }

    private String getStackTrace(ActorRef actor) {
        final StackTraceElement[] stackTrace = LocalActor.getStackTrace(actor);
        final StringBuilder sb = new StringBuilder();
        for (StackTraceElement ste : stackTrace)
            sb.append(ste).append('\n');
        return sb.toString();
    }
    
    @Override
    public String[] getMailbox(long actorId) {
        ActorRef actor = actors.get(actorId);
        if (actor == null)
            return null;

        return getMailbox(actor);
    }

    private String[] getMailbox(ActorRef actor) {
        List<Object> list = LocalActor.getMailboxSnapshot(actor);
        String[] ms = new String[list.size()];
        int i = 0;
        for (Object m : list)
            ms[i++] = m.toString();
        return ms;
    }
    
    @Override
    public void addWatch(long actorId) {
        ActorRef actor = actors.get(actorId);
        if (actor == null)
            throw new IllegalArgumentException("No actor with id " + actorId + " found.");
        SmallActorMonitor mon = new SmallActorMonitor(actorId);
        LocalActor.setMonitor(actor, mon);
        watchedActors.put(actorId, mon);
    }

    @Override
    public void removeWatch(long actorId) {
        SmallActorMonitor mon = watchedActors.get(actorId);
        if (mon == null)
            return;
        LocalActor.stopMonitor(mon.actor);
    }

    @Override
    public List<ActorInfo> getWatchedActorsInfo() {
        List<ActorInfo> list = new ArrayList<>(watchedActors.size());
        for (SmallActorMonitor mon : watchedActors.values()) {
            ActorInfo ai = getActorInfo(mon);
            if (ai != null)
                list.add(ai);
        }
        Collections.sort(list, new Comparator<ActorInfo>() {
            public int compare(ActorInfo o1, ActorInfo o2) {
                return (int) (o1.getId() - o2.getId());
            }
        });
        return list;
    }

    private ActorInfo getActorInfo(SmallActorMonitor mon) {
        final ActorRef actor = mon.actor;
        if (actor == null)
            return null;
        return new ActorInfo(mon.id,
                actor.getName(),
                LocalActor.getStrand(actor).isFiber(),
                mon.messages,
                LocalActor.getQueueLength(actor),
                (int) mon.restartCounter.get(),
                mon.deathCauses.toArray(new String[0]),
                getMailbox(actor),
                getStackTrace(actor));
    }

    private class SmallActorMonitor implements ActorMonitor {
        private final long id;
        private ActorRef actor;
        private int messageCounter;
        private int skippedMessageCounter;
        private final Counter restartCounter = new Counter();
        private final Queue<String> deathCauses = new ConcurrentLinkedQueue<>();
        // These hold counter values for the previous window
        volatile long messages;
        volatile long skippedMessages;

        public SmallActorMonitor(long id) {
            this.id = id;
        }

        void refresh() {
            if (registered) {
                final ActorRef a1 = actor;
                if (a1 != null)
                    collect(nanoTime() - lastCollectTime);

                reset();
            }
        }

        protected void collect(long intervalNanos) {
            messages = messageCounter;
        }

        protected void reset() {
            messageCounter = 0;

            lastCollectTime = nanoTime();
        }

        @Override
        public void setActor(ActorRef actor) {
            this.actor = actor;
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

        @Override
        public void shutdown() {
            watchedActors.remove(id);
        }
    }
}
