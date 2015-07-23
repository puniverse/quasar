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

import static co.paralleluniverse.continuation.CoIterable.*;
import static co.paralleluniverse.continuation.CoIterables.*;
import co.paralleluniverse.fibers.Fiber;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pron
 */
public class CoIterableTest {

    public CoIterableTest() {
    }

    @Test
    public void test1() {
        assertEquals(new ArrayList<>(Arrays.asList(1000, 1001, 1002, 1003, 1004)), toList(take(5, range(1000))));
    }

    @Test
    public void test2() {
        assertEquals(toList(range(1000, 1010)), toList(take(10, range(1000))));
    }

    @Test
    public void testFlatMap() {
        assertEquals(45, toList(flatMap(range(0, 10), x -> range(0, x))).size());
    }

    @Test
    public void testInFiber() throws Exception {
        Fiber<Integer> f = new Fiber<>(() -> {
            for (int x : new CoIterable<Integer>(() -> {
                Fiber.sleep(20);
                produce(1);
                Fiber.sleep(20);
                produce(11);
                Fiber.sleep(20);
                produce(111);
                Fiber.sleep(20);
                produce(1111);
            }))
                if (x > 100)
                    return x;
            return 0;
        }).start();

        assertEquals((Integer) 111, f.get());
    }

    static Iterable<Integer> range(int from, int to) {
        return new CoIterable<>(() -> {
            for (int i = from; i < to; i++)
                produce(i);
        });
    }

    static Iterable<Integer> range(int from) {
        return new CoIterable<>(() -> {
            for (int i = from;; i++)
                produce(i);
        });
    }

    private static <E> List<E> toList(Iterable<E> it) {
        List<E> list = new ArrayList<E>();
        Iterables.addAll(list, it);
        return list;
    }
}
