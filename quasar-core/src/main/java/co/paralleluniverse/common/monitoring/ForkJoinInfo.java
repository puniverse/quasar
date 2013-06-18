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

import java.beans.ConstructorProperties;

/**
 *
 * @author pron
 */
public final class ForkJoinInfo {
    private final int activeThreadCount; // Returns an estimate of the number of threads that are currently stealing or executing tasks.
    private final int runningThreadCount; // Returns an estimate of the number of worker threads that are not blocked waiting to join tasks or for other managed synchronization.
    private final int queuedSumbmissionCount; // Returns an estimate of the number of tasks submitted to this pool that have not yet begun executing.
    private final long queuedTaskCount; // Returns an estimate of the total number of tasks currently held in queues by worker threads (but not including tasks submitted to the pool that have not begun executing).
    private final long stealCount; // Returns an estimate of the total number of tasks stolen from one thread's work queue by another.

    @ConstructorProperties({"activeThreadCount", "runningThreadCount", "queuedSumbmissionCount", "queuedTaskCount", "stealCount"})
    public ForkJoinInfo(int activeThreadCount, int runningThreadCount, int queuedSumbmissionCount, long queuedTaskCount, long stealCount) {
        this.activeThreadCount = activeThreadCount;
        this.runningThreadCount = runningThreadCount;
        this.queuedSumbmissionCount = queuedSumbmissionCount;
        this.queuedTaskCount = queuedTaskCount;
        this.stealCount = stealCount;
    }

    /**
     * @return An estimate of the number of threads that are currently stealing or executing tasks.
     */
    public int getActiveThreadCount() {
        return activeThreadCount;
    }

    /**
     * @return An estimate of the number of worker threads that are not blocked waiting to join tasks or for other managed synchronization.
     */
    public int getRunningThreadCount() {
        return runningThreadCount;
    }

    /**
     * @return An estimate of the number of tasks submitted to this pool that have not yet begun executing.
     */
    public int getQueuedSumbmissionCount() {
        return queuedSumbmissionCount;
    }

    /**
     * @return An estimate of the total number of tasks currently held in queues by worker threads (but not including tasks submitted to the pool that have not begun executing).
     */
    public long getQueuedTaskCount() {
        return queuedTaskCount;
    }

    /**
     * @return An estimate of the total number of tasks stolen from one thread's work queue by another.
     */
    public long getStealCount() {
        return stealCount;
    }
}
