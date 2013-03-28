/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.common.monitoring;

import com.google.common.collect.MapMaker;
import java.lang.ref.WeakReference;
import java.util.Map;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public abstract class ForkJoinPoolMonitor {
    private static final Map<ForkJoinPool, ForkJoinPoolMonitor> instances = new MapMaker().weakKeys().makeMap();
    
    public static ForkJoinPoolMonitor getInstacnce(ForkJoinPool fjp) {
        return instances.get(fjp);
    }
    
    private final WeakReference<ForkJoinPool> fjPool;
    private final String name;

    public ForkJoinPoolMonitor(String name, ForkJoinPool fjPool) {
        //super(ForkJoinPoolMXBean.class, true, new NotificationBroadcasterSupport());
        this.name = "co.paralleluniverse:type=SpaceBase,name=" + name + ",monitor=forkJoinPool";
        this.fjPool = new WeakReference<ForkJoinPool>(fjPool);
        instances.put(fjPool, this);
    }
    
    protected ForkJoinPool fjPool() {
        final ForkJoinPool fjPool = this.fjPool.get();
        return fjPool;
    }

    public abstract void doneTask(int runs);
}
