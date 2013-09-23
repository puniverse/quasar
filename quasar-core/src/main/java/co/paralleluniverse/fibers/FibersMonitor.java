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

/**
 *
 * @author pron
 */
public interface FibersMonitor {

    void fiberStarted();

    void fiberSubmitted(boolean start);

    void fiberSuspended();

    void fiberTerminated();
    
    void spuriousWakeup();
    
    void timedParkLatency(long ns);
    
    void unregister();
}
