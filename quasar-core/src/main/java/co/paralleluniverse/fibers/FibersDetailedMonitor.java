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
package co.paralleluniverse.fibers;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author pron
 */
class FibersDetailedMonitor {
    private final Map<Long, Fiber<?>> fibers = new ConcurrentHashMap<>();

    void fiberStarted(Fiber<?> fiber) {
        fibers.put(fiber.getId(), fiber);
    }

    void fiberTerminated(Fiber<?> fiber) {
        fibers.remove(fiber.getId());
    }

    public long[] getAllFiberIds() {
        int size = fibers.size();
        long[] ids = new long[size];
        int i = 0;
        for (long id : fibers.keySet()) {
            ids[i] = id;
            i++;
        }
        if (i < size)
            return Arrays.copyOf(ids, i);
        else
            return ids; // might not include all nw fibers
    }

    public FiberInfo getFiberInfo(long id, boolean stack) {
        final Fiber<?> f = fibers.get(id);
        if (f == null)
            return null;
        return f.getFiberInfo(stack);
    }

    public FiberInfo[] getFiberInfo(long[] ids, boolean stack) {
        FiberInfo[] fis = new FiberInfo[ids.length];
        for (int i = 0; i < ids.length; i++)
            fis[i] = getFiberInfo(ids[i], stack);
        return fis;
    }
}
