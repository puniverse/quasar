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

import co.paralleluniverse.strands.concurrent.ReentrantLock;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

/**
 * Associates a value with the current {@link FiberScheduler}
 *
 * @author pron
 */
public class SchedulerLocal<T> {
    private final Lock lock = new ReentrantLock();

    /**
     * Computes the initial value for the current {@link FiberScheduler}.
     * Returns {@code null} by default;
     *
     * @param scheduler the current {@link FiberScheduler}
     * @return the initial value
     */
    protected T initialValue(FiberScheduler scheduler) {
        return null;
    }

    /**
     * Returns the scheduler-local value of this {@code SchedulerLocal}.
     */
    public final T get() throws SuspendExecution {
        final FiberScheduler scheduler = currentScheduler();
        final ConcurrentMap<SchedulerLocal, Entry<?>> map = scheduler.schedLocals;
        Entry<T> entry = (Entry<T>) map.get(this);
        if (entry == null) {
            lock.lock();
            try {
                entry = (Entry<T>) map.get(this);
                if (entry == null) {
                    entry = new Entry<>();
                    entry.value = initialValue(scheduler);
                    Entry<?> old = map.putIfAbsent(this, entry);
                    assert old == null;
                }
            } finally {
                lock.unlock();
            }
        }
        return entry.value;
    }

    /**
     * Sets the scheduler-local value of this {@code SchedulerLocal}.
     */
    public final void set(T value) {
        getEntry(getMap()).value = value;
    }

    /**
     * Removes the association between the scheduler-local value and the current scheduler.
     * The next call to {@link #get()} would return the initial value returned by a fresh call to {@link #initialValue(FiberScheduler) initialValue}.
     */
    public final void remove() {
        getMap().remove(this);
    }

    private static ConcurrentMap<SchedulerLocal, Entry<?>> getMap() {
        return currentScheduler().schedLocals;
    }

    private Entry<T> getEntry(ConcurrentMap<SchedulerLocal, Entry<?>> map) {
        Entry<T> entry = (Entry<T>) map.get(this);
        if (entry == null) {
            entry = new Entry<>();
            Entry<?> old = map.putIfAbsent(this, entry);
            if (old != null)
                entry = (Entry<T>) old;
        }
        return entry;
    }

    private static FiberScheduler currentScheduler() {
        final Fiber<?> currentFiber = Fiber.currentFiber();
        if (currentFiber == null)
            throw new IllegalStateException("Method called not within a fiber");
        return currentFiber.getScheduler();
    }

    static class Entry<T> {
        T value;
    }
}
