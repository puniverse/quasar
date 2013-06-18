/*
 * Copyright (C) 2011-2013, Parallel Universe Software Co. All rights reserved.
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
