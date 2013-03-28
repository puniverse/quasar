/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.common.monitoring;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public class JMXForkJoinPoolMonitor extends ForkJoinPoolMonitor implements ForkJoinPoolMXBean {
    private final String mbeanName;
    private boolean registered;
    private Map<?, Integer> highContentionObjects;

    public JMXForkJoinPoolMonitor(String name, ForkJoinPool fjPool, Map<?, Integer> highContentionObjects) {
        super(name, fjPool);
        this.highContentionObjects = highContentionObjects;
        //super(ForkJoinPoolMXBean.class, true, new NotificationBroadcasterSupport());
        this.mbeanName = "co.paralleluniverse:type=SpaceBase,name=" + name + ",monitor=forkJoinPool";
        registerMBean();
    }

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
    }

    public void unregisterMBean() {
        try {
            if (registered)
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(mbeanName));
            this.registered = false;
        } catch (InstanceNotFoundException ex) {
            ex.printStackTrace();
        } catch (MBeanRegistrationException ex) {
            ex.printStackTrace();
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

    public static enum Status {
        ACTIVE, QUIESCENT, SHUTDOWN, TERMINATING, TERMINATED
    }

    @Override
    protected ForkJoinPool fjPool() {
        final ForkJoinPool fjPool = super.fjPool();
        if (fjPool == null) {
            unregisterMBean();
            throw new RuntimeException("Pool collected");
        }
        return fjPool;
    }

    @Override
    public Status getStatus() {
        final ForkJoinPool fjPool = fjPool();
        if (fjPool.isTerminated()) // Returns true if all tasks have completed following shut down.
            return Status.TERMINATED;
        if (fjPool.isTerminating()) // Returns true if the process of termination has commenced but not yet completed.
            return Status.TERMINATING;
        if (fjPool.isShutdown()) // Returns true if this pool has been shut down.
            return Status.SHUTDOWN;
        if (fjPool.isQuiescent()) // Returns true if all worker threads are currently idle.
            return Status.QUIESCENT;
        return Status.ACTIVE;
    }

    @Override
    public boolean getAsyncMode() {
        return fjPool().getAsyncMode(); // Returns true if this pool uses local first-in-first-out scheduling mode for forked tasks that are never joined.
    }

    @Override
    public int getParalellism() {
        return fjPool().getParallelism(); // Returns the targeted parallelism level of this pool.
    }

    @Override
    public int getPoolSize() {
        return fjPool().getPoolSize(); // Returns the number of worker threads that have started but not yet terminated.
    }

    @Override
    public ForkJoinInfo getInfo() {
        final ForkJoinPool fjPool = fjPool();
        int activeThreadCount = fjPool.getActiveThreadCount(); // Returns an estimate of the number of threads that are currently stealing or executing tasks.
        int runningThreadCount = fjPool.getRunningThreadCount(); // Returns an estimate of the number of worker threads that are not blocked waiting to join tasks or for other managed synchronization.
        int queuedSumbmissionCount = fjPool.getQueuedSubmissionCount(); // Returns an estimate of the number of tasks submitted to this pool that have not yet begun executing.
        long queuedTaskCount = fjPool.getQueuedTaskCount(); // Returns an estimate of the total number of tasks currently held in queues by worker threads (but not including tasks submitted to the pool that have not begun executing).
        long stealCount = fjPool.getStealCount(); //  Returns an estimate of the total number of tasks stolen from one thread's work queue by another.

        return new ForkJoinInfo(activeThreadCount, runningThreadCount, queuedSumbmissionCount, queuedTaskCount, stealCount);
    }

    @Override
    public void shutdown() {
        fjPool().shutdown();
    }

    @Override
    public void shutdownNow() {
        fjPool().shutdownNow();
    }

    @Override
    public Map<String, Integer> getHighContentionLocks() {
        if(highContentionObjects == null)
            return null;
        final Map<String, Integer> map = new HashMap<String, Integer>();
        for(Map.Entry<?, Integer> entry : highContentionObjects.entrySet())
            map.put(entry.getKey().toString(), entry.getValue());
        return map;
    }
    
    @Override
    public void doneTask(int runs) {
    }
}
