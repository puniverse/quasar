/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers;

import co.paralleluniverse.strands.Strand.State;
import java.util.Map;

/**
 * An MXBean that monitors fibers scheduled by a single {@link FiberScheduler}.
 *
 * @author pron
 */
public interface FibersMXBean {
    void refresh();

    /**
     * The number of non-terminated fibers in the scheduler.
     */
    int getNumActiveFibers();

    /**
     * The number of fibers currently in the {@link State#RUNNING RUNNING} state.
     */
    int getNumRunnableFibers();

    /**
     * The number of fibers that are currently blocking.
     */
    int getNumWaitingFibers();

    /**
     * The fibers (and respective stack traces) that are currently holding their thread for a lengthy duration, either due to blocking
     * or a lengthy loop.
     */
    Map<String, String> getRunawayFibers();
    
    long getSpuriousWakeups();

    /**
     * The average latency between the time fibers in the scheduler have asked to be awakened (by a {@code sleep} or any other timed wait)
     * and the time they've actually been awakened in the last 5 seconds.
     */
    long getMeanTimedWakeupLatency();

    /**
     * The IDs of all fibers in the scheduler. {@code null} if the scheduler has been constructed with {@code detailedInfo} equal to {@code false}.
     */
    long[] getAllFiberIds();

    /**
     * Returns a {@link FiberInfo} object for a single fiber. Returns {@code null} if the scheduler has been constructed with {@code detailedInfo} equal to {@code false}.
     *
     * @param id    the fiber's id.
     * @param stack whether the fiber's call stack is required.
     * @return a {@link FiberInfo} object for a single fiber, or {@code null} if the scheduler has been constructed with {@code detailedInfo} equal to {@code false}.
     */
    FiberInfo getFiberInfo(long id, boolean stack);

    /**
     * Returns an array {@link FiberInfo} objects for a set of fibers. Returns {@code null} if the scheduler has been constructed with {@code detailedInfo} equal to {@code false}.
     *
     * @param ids   the fibers' ids.
     * @param stack whether the fibers' call stack is required.
     * @return an array {@link FiberInfo} objects for a set of fibers, or {@code null} if the scheduler has been constructed with {@code detailedInfo} equal to {@code false}.
     */
    FiberInfo[] getFiberInfo(long[] ids, boolean stack);
}
