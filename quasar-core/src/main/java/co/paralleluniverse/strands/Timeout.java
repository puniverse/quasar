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
package co.paralleluniverse.strands;

import java.util.concurrent.TimeUnit;

/**
 * Represents a timeout that can span several operations.
 * This object expires within a given time from the instant this object has been created.
 * It maintains the time left until it expires, so it can be initialized once and then passed to several consecutive
 * operation, making sure that the sequence of operations completes within the timeout's duration.
 * <p/>
 * To use this timeout in methods that take a {@code long timeout} and a {@code TimeUnit unit} argument, pass:
 * <pre>{@code
 *     timeout.nanosLeft(), TimeUnit.NANOSECONDS
 * }</pre>
 *
 * @author pron
 */
public class Timeout {
    private final long deadline;

    /**
     * Starts a new {@code Timeout} that begins now and expires within the given timeout
     * from the instant this constructor has been called.
     *
     * @param timeout the duration of the timeout
     * @param unit    the timeout's time unit
     */
    public Timeout(long timeout, TimeUnit unit) {
        this.deadline = System.nanoTime() + unit.toNanos(timeout);
    }

    /**
     * Returns how many nanoseconds are left before the timeout expires,
     * or a negative number indicating how many nanoseconds have elapsed since
     * the timeout expired.
     */
    public long nanosLeft() {
        return deadline - System.nanoTime();
    }

    /**
     * Returns how long is left before the timeout expires in the given time unit.
     *
     * @param unit the time unit of the return value
     * @return how long is left before the timeout expires in the given time unit.
     */
    public long timeLeft(TimeUnit unit) {
        return unit.convert(nanosLeft(), TimeUnit.NANOSECONDS);
    }

    /**
     * Tests whether the timeout has expired.
     */
    public boolean isExpired() {
        return nanosLeft() <= 0;
    }
}
