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
package co.paralleluniverse.continuation;

import static co.paralleluniverse.continuation.CoIterable.produce;
import com.google.common.base.Function;
import java.util.function.Predicate;

public class CoIterables {
    public static <E> Iterable<E> filter(Iterable<E> it, Predicate<E> p) {
        return new CoIterable<>(() -> {
            for (E x : it)
                if (p.test(x))
                    produce(x);
        });
    }

    public static <T, R> Iterable<T> map(Iterable<R> it, Function<R, T> m) {
        // Unfortunately, throwables can't be generic. We would have wanted to CoIteratorScope<E>. Now, type checking is lacking
        return new CoIterable<>(() -> {
            for (R x : it)
                produce(m.apply(x));
        });
    }

    public static <T, R> Iterable<T> flatMap(Iterable<R> it, Function<R, Iterable<T>> m) {
        // Unfortunately, throwables can't be generic. We would have wanted to CoIteratorScope<E>. Now, type checking is lacking
        return new CoIterable<>(() -> {
            for (R x : it)
                for (T y : m.apply(x))
                    produce(y);
        });
    }

    public static <E> Iterable<E> take(int n, Iterable<E> it) {
        return new CoIterable<>(() -> {
            int i = 0;
            for (E x : it) {
                if (i++ >= n)
                    break;
                produce(x);
            }
        });
    }
}
