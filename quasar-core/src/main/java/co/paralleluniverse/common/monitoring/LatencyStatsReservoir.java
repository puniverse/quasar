/*
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
/*
 * Copyright 2014 Marshall Pierce
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.paralleluniverse.common.monitoring;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.LatencyUtils.LatencyStats;

/**
 *
 * @author pron
 */
public class LatencyStatsReservoir implements Reservoir {
    private final LatencyStats stats;
    private final Histogram runningTotals;
    private final Histogram intervalHistogram;

    public LatencyStatsReservoir(LatencyStats stats) {
        this.stats = stats;
        intervalHistogram = stats.getIntervalHistogram();
        runningTotals = new Histogram(intervalHistogram.getNumberOfSignificantValueDigits());
    }

    @Override
    public int size() {
        return getSnapshot().size();
    }

    @Override
    public void update(long value) {
        stats.recordLatency(value);
    }

    @Override
    public Snapshot getSnapshot() {
        return new HistogramSnapshot(updateRunningTotals());
    }

    private synchronized Histogram updateRunningTotals() {
        stats.addIntervalHistogramTo(runningTotals);
        return runningTotals.copy();
    }
}
