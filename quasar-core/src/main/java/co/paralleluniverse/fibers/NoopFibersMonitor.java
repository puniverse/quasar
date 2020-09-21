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

import java.util.Collection;

/**
 *
 * @author pron
 */
public class NoopFibersMonitor implements FibersMonitor {
    public NoopFibersMonitor() {
    }

    @Override
    public void fiberStarted(Fiber<?> fiber) {
    }

    @Override
    public void fiberResumed() {
    }

    @Override
    public void fiberSuspended() {
    }

    @Override
    public void fiberTerminated(Fiber<?> fiber) {
    }

    @Override
    public void spuriousWakeup() {
    }

    @Override
    public void timedParkLatency(long ns) {
    }

    @Override
    public void setRunawayFibers(Collection<Fiber<?>> fs) {
    } 

    @Override
    public void unregister() {
    }
}
