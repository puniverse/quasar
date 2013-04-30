/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.common.monitoring.Counter;
import co.paralleluniverse.common.monitoring.MonitoringServices;
import co.paralleluniverse.common.util.Objects;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
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

/**
 *
 * @author pron
 */
public class JMXActorMonitor extends StandardEmitterMBean implements ActorMonitor, ActorMXBean, NotificationListener, NotificationEmitter {
    /*
     * For the time being, we're not worried about data races. Messages counters are all updated by the actor, so there's no problem there.
     * For the JMX thread to see the messages counter, it should really be volatile, but as an approximation, we keep it a regular int.
     */
    private WeakReference<Actor> actor;
    private final String name;
    private boolean registered;
    private long lastCollectTime;
    private int notificationSequenceNumber;
    //
    private int messageCounter;
    private int skippedMessageCounter;
    private final Counter restartCounter = new Counter();
    private final Queue<String> deathReasons = new ConcurrentLinkedQueue<>();
    // These hold counter values for the previous window
    private long messages;
    private long skippedMessages;
    //

    public JMXActorMonitor(String name) {
        super(ActorMXBean.class, true, new NotificationBroadcasterSupport());
        this.name = "co.paralleluniverse:type=quasar,monitor=actor,name=" + name;
        lastCollectTime = nanoTime();
        collectAndResetCounters();
        registerMBean();
    }

    @Override
    public void setActor(Actor actor) {
        this.actor = new WeakReference<Actor>(actor);
    }

    private void registerMBean() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName mxbeanName = new ObjectName(name);
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
        collectAndResetCounters();
    }

    private void collectAndResetCounters() {
        if (registered) {
            if (actor != null && actor.get() == null)
                unregisterMBean();
            else
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
    public final long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public void addDeath(Object reason) {
        while (deathReasons.size() > 20)
            deathReasons.poll();
        deathReasons.add(reason.toString());
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
        final Actor a = this.actor.get();
        if (a == null) {
            unregisterMBean();
            return 0;
        }
        return a.getQueueLength();
    }

    @Override
    public int getTotalReceivedMessages() {
        return (int) messages;
    }

    @Override
    public int getTotalRestarts() {
        return (int) restartCounter.get();
    }

    @Override
    public String[] getLastDeathReasons() {
        return deathReasons.toArray(new String[0]);
    }
}
