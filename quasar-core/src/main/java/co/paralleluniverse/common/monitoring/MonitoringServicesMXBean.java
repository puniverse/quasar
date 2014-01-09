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
public interface MonitoringServicesMXBean {

    int getPerformanceTimerPeriod();

    int getStructuraltimerPeriod();

    boolean isPerformanceUpdates();

    boolean isStructuralUpdates();

    void setPerformanceTimerPeriod(int perfTimerPeriod);

    void setStructuraltimerPeriod(int structuraltimerPeriod);

    void startPerformanceUpdates();

    void startStructuralUpdates();

    void stopPerformanceUpdates();

    void stopStructuralUpdates();

    void setPerformanceUpdates(boolean value);

    void setStructuralUpdates(boolean value);
}
