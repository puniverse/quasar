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

/**
 *
 * @author pron
 */
public interface ForkJoinPoolMXBean {
    boolean getAsyncMode();

    int getParallelism();

    int getPoolSize();

    ForkJoinPoolMonitor.Status getStatus();

    int getActiveThreadCount();

    int getRunningThreadCount();

    int getQueuedSubmissionCount();

    long getQueuedTaskCount();

    long getStealCount();

    long[] getLatency();

    ForkJoinInfo getInfo();

    void shutdown();

    void shutdownNow();
}
