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
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author pron
 */
public abstract class DelayedValue implements Delayed {
    public static DelayedValue instance(boolean sequenced, int value, long millis) {
        return sequenced ? new DelayedValue1(value, millis) : new DelayedValue2(value, millis);
    }
    final long time;
    private final int value;

    DelayedValue(int value, long millis) {
        this.time = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(time - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (int) (this.time ^ (this.time >>> 32));
        hash = 13 * hash + this.value;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DelayedValue other = (DelayedValue) obj;
        if (this.time != other.time)
            return false;
        if (this.value != other.value)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DelayedValue{" + "time=" + time + ", value=" + value + '}';
    }

    static class DelayedValue1 extends DelayedValue {
        /**
         * Sequence number to break scheduling ties, and in turn to
         * guarantee FIFO order among tied entries.
         */
        private static final AtomicLong sequencer = new AtomicLong();
        private final long sequenceNumber;

        DelayedValue1(int value, long millis) {
            super(value, millis);
            this.sequenceNumber = sequencer.getAndIncrement();
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) // compare zero if same object
                return 0;
            if (other instanceof DelayedValue1) {
                DelayedValue1 x = (DelayedValue1) other;
                long diff = time - x.time;
                if (diff < 0)
                    return -1;
                else if (diff > 0)
                    return 1;
                else if (sequenceNumber < x.sequenceNumber)
                    return -1;
                else
                    return 1;
            }
            long diff = getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
        }
    }

    static class DelayedValue2 extends DelayedValue {
        public DelayedValue2(int value, long millis) {
            super(value, millis);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) // compare zero if same object
                return 0;
            if (other instanceof DelayedValue2) {
                DelayedValue2 x = (DelayedValue2) other;
                long diff = time - x.time;
                return (int) diff; // (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
            }
            long diff = getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
            return (int) diff;
        }
    }
}
