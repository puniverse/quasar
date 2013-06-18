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

import co.paralleluniverse.common.monitoring.JMXForkJoinPoolMonitor.Status;
import java.util.Map;

/**
 *
 * @author pron
 */
public interface ForkJoinPoolMXBean {

    boolean getAsyncMode();

    ForkJoinInfo getInfo();

    int getParalellism();

    int getPoolSize();

    Status getStatus();

    void shutdown();

    void shutdownNow();
    
    Map<String, Integer> getHighContentionLocks();
}
