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

import java.lang.ref.WeakReference;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public abstract class ForkJoinPoolMonitor {
    private final WeakReference<ForkJoinPool> fjPool;

    public ForkJoinPoolMonitor(String name, ForkJoinPool fjPool) {
        this.fjPool = new WeakReference<ForkJoinPool>(fjPool);
    }
    
    public void unregister() {
    }
    
    protected ForkJoinPool fjPool() {
        final ForkJoinPool fjp = this.fjPool.get();
        return fjp;
    }
}
