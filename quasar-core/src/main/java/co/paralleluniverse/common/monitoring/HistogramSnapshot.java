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

import com.codahale.metrics.Snapshot;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import static java.nio.charset.StandardCharsets.UTF_8;

final class HistogramSnapshot extends Snapshot {
    private final Histogram histogram;

    HistogramSnapshot(Histogram histogram) {
        this.histogram = histogram;
    }

    @Override
    public double getValue(double quantile) {
        return histogram.getValueAtPercentile(quantile * 100.0);
    }

    @Override
    public long[] getValues() {
        final long[] vals = new long[(int) histogram.getTotalCount()];
        int i = 0;
        for (HistogramIterationValue value : histogram.recordedValues()) {
            long val = value.getValueIteratedTo();
            for (int j = 0; j < value.getCountAddedInThisIterationStep(); j++)
                vals[i++] = val;
        }
        if (i != vals.length)
            throw new IllegalStateException("Total count was " + histogram.getTotalCount() + " but iterating values produced " + vals.length);

        return vals;
    }

    @Override
    public int size() {
        return (int) histogram.getTotalCount();
    }

    @Override
    public long getMax() {
        return histogram.getMaxValue();
    }

    @Override
    public double getMean() {
        return histogram.getMean();
    }

    @Override
    public long getMin() {
        return histogram.getMinValue();
    }

    @Override
    public double getStdDev() {
        return histogram.getStdDeviation();
    }

    @Override
    public void dump(OutputStream output) {
        PrintWriter p = new PrintWriter(new OutputStreamWriter(output, UTF_8));
        for (HistogramIterationValue value : histogram.recordedValues()) {
            for (int j = 0; j < value.getCountAddedInThisIterationStep(); j++)
                p.printf("%d%n", value.getValueIteratedTo());
        }
    }
}
