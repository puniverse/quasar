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
public interface FibersMonitor {

    void fiberStarted(Fiber<?> fiber);

    void fiberResumed();

    void fiberSuspended();

    void fiberTerminated(Fiber<?> fiber);
    
    void spuriousWakeup();
    
    void timedParkLatency(long ns);
    
    void unregister();
    
    void setRunawayFibers(Collection<Fiber<?>> fs);
}
