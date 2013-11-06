/*
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
package co.paralleluniverse.common.monitoring;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import jsr166e.LongAdder;
import jsr166e.LongMaxUpdater;

/**
 * This is a false reservoir that merely records the precise max and min, as well as an approximate mean.
 * 
 * @author pron
 */
public class SimpleReservoir implements Reservoir {
    private LongAdder num = new LongAdder();
    private LongAdder sum = new LongAdder();
    private LongMaxUpdater max = new LongMaxUpdater();
    private LongMaxUpdater min = new LongMaxUpdater();

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void update(long value) {
        num.increment();
        sum.add(value);
        max.update(value);
        min.update(-value);
    }

    @Override
    public Snapshot getSnapshot() {
        return new Snapshot(new long[0]) {
            private final long num = SimpleReservoir.this.num.sumThenReset();
            private final long sum = SimpleReservoir.this.sum.sumThenReset();
            private final long max = SimpleReservoir.this.max.maxThenReset();
            private final long min = SimpleReservoir.this.min.maxThenReset();

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
                return -min;
            }

            @Override
            public double getMean() {
                return (double) sum / (double) num;
            }
        };
    }
}
