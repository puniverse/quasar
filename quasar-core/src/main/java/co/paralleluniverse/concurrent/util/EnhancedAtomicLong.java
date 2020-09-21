/*
 * Quasar: lightweight threads and actors for the JVM.
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
package co.paralleluniverse.concurrent.util;

import co.paralleluniverse.fibers.instrument.DontInstrument;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Extension of {@link AtomicLong} with useful methods based on CAS.
 * This class doesn't need to be instrumented, but it has lambdas
 * and so Quasar will do it anyway. Hence {@link DontInstrument}!
 * @author circlespainter
 */
@DontInstrument
public class EnhancedAtomicLong extends AtomicLong {
    public static final Function<Long, Long> DEC = l -> l - 1;

    public static Predicate<Long> gt(final long n) {
        return l -> l > n;
    }

    public boolean evalAndUpdate(final Predicate<Long> predicate, final Function<Long, Long> update) {
        long val;
        boolean satisfied;
        do {
            val = get();
            satisfied = predicate.test(val);
        } while (satisfied && !compareAndSet(val, update.apply(val)));
        return satisfied;
    }
}
