/*
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
package co.paralleluniverse.common.monitoring;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * This is a false reservoir that merely records the precise max and min, as well as an approximate mean.
 * 
 * @author pron
 */
public class SimpleReservoir implements Reservoir {
    private final LongAdder num = new LongAdder();
    private final LongAdder sum = new LongAdder();
    private final LongAccumulator max = new LongAccumulator(Math::max, 0);
    private final LongAccumulator min = new LongAccumulator(Math::min, Long.MAX_VALUE);

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void update(long value) {
        num.increment();
        sum.add(value);
        max.accumulate(value);
        min.accumulate(value);
    }

    @Override
    public Snapshot getSnapshot() {
        return new Snapshot(new long[0]) {
            private final long num = SimpleReservoir.this.num.sumThenReset();
            private final long sum = SimpleReservoir.this.sum.sumThenReset();
            private final long max = SimpleReservoir.this.max.getThenReset();
            private final long min = SimpleReservoir.this.min.getThenReset();

            @Override
            public int size() {
                return 0;
            }

            @Override
            public long getMax() {
                return max;
            }

            @Override
            public long getMin() {
                return min;
            }

            @Override
            public double getMean() {
                return (double) sum / (double) num;
            }
        };
    }
}
