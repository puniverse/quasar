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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Extension of {@link AtomicLong} with useful methods based on CAS.
 *
 * @author circlespainter
 */
public class EnhancedAtomicLong extends AtomicLong {
    public static final Function<Long, Long> DEC =
        new Function<Long, Long>() {
            @Override
            public Long apply(final Long l) {
                return l - 1;
            }                
        };

    public static Predicate<Long> gt(final long n) {
        return new Predicate<Long>() {
            public boolean apply(final Long l) {
                return l > n;
            }
        };
    }

    public boolean evalAndUpdate(final Predicate<Long> predicate, final Function<Long, Long> update) {
        long val;
        boolean satisfied;
        do {
            val = get();
            satisfied = predicate.apply(val);
        } while (satisfied && !compareAndSet(val, update.apply(val)));
        return satisfied;
    }
}
