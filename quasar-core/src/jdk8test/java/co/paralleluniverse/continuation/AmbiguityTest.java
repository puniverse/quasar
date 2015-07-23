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

import static co.paralleluniverse.continuation.Ambiguity.*;
import static co.paralleluniverse.continuation.CoIterable.*;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pron
 */
public class AmbiguityTest {

    public AmbiguityTest() {
    }

    @Test
    public void test1() throws Exception {
        Ambiguity<Integer> amb = solve(() -> {
            int a = amb(1, 2, 3);
            return a;
        });

        assertEquals((Integer) 1, amb.run());
        assertTrue(amb.hasRemaining());
        assertEquals((Integer) 2, amb.run());
        assertTrue(amb.hasRemaining());
        assertEquals((Integer) 3, amb.run());
        assertTrue(!amb.hasRemaining());
        try {
            amb.run();
            fail();
        } catch (NoSolution e) {
        }
    }

    @Test
    public void test2() {
        Ambiguity<Integer> amb = solve(() -> {
            int a = amb(1, 2, 3, 4);
            return a;
        });

        assertEquals(list(1, 2, 3, 4), list(solutions(amb)));
    }

    @Test
    public void test3() {
        Ambiguity<String> amb = solve(() -> {
            int a = amb(1, 2);
            String b = amb("a", "b");
            return b + a;
        });

        assertEquals(list("a1", "b1", "a2", "b2"), list(solutions(amb)));
    }

    @Test
    public void test4() {
        Ambiguity<Integer> amb = solve(() -> {
            int a = amb(1, 2, 3);
            int b = amb(2, 3, 4);

            assertThat(b < a);
            return b;
        });

        assertEquals(list(2), list(solutions(amb)));
    }

    @Test
    public void test4InFiber() throws Exception {
        Fiber<List<Integer>> f = new Fiber<>(() -> {
            Ambiguity<Integer> amb = solve(() -> {
                int a = amb(1, 2, 3);
                int b = amb(2, 3, 4);

                Fiber.sleep(20);
                assertThat(b < a);
                return b;
            });

            return list(solutions(amb));
        }).start();

        assertEquals(list(2), f.get());
    }

    @Test
    public void test5() {
        Ambiguity<Integer> amb = solve(() -> {
            int a = amb(1, 2, 3);
            int b = amb(3, 4);

            assertThat(b < a);
            return b;
        });

        assertEquals(list(), list(solutions(amb)));
    }

    @Test
    public void test6() throws Exception {
        Ambiguity<Integer> amb = solve(() -> {
            Iterable<Integer> a = iterable(() -> {
                produce(amb(2, 1));
                produce(amb(3, 10));
            });

            System.err.println("VVVVV0");
            int sum = 0;
            for (int x : a) {
                sum += x;
                System.err.println("VVVVV1 ASSERTING: " + x + " " + sum);
                assertThat(x % 2 == 0);
                System.err.println("VVVVV2 ASSERTED: " + x + " " + sum);
            }
            
            System.err.println("TYTYTYTYTYTYTY " + sum);
            return sum;
        });

        assertEquals(list(12), list(solutions(amb)));
    }

    @Test
    public void test6InFiber() throws Exception {
        Fiber<List<Integer>> f = new Fiber<>(() -> {
            Ambiguity<Integer> amb = solve(() -> {
                Iterable<Integer> a = iterable(() -> {
                    produce(amb(2, 1));
                    Fiber.sleep(20);
                    produce(amb(3, 10));
                });

                int sum = 0;
                for (int x : a) {
                    sum += x;
                    Fiber.sleep(20);
                    assertThat(x % 2 == 0);
                }
                return sum;
            });
            return list(solutions(amb));
        }).start();

        assertEquals(list(12), f.get());
    }

    static <T> Iterable<T> solutions(Ambiguity<T> amb) {
        return iterable(() -> {
            try {
                while (amb.hasRemaining())
                    produce(amb.run());
            } catch (NoSolution e) {
            }
        });
    }

    @Suspendable
    private static <E> List<E> list(Iterable<E> it) {
        List<E> list = new ArrayList<E>();
        addAll(list, it);
        return list;
    }

    private static <E> List<E> list(E... xs) {
        return new ArrayList<E>(Arrays.asList(xs));
    }

    @Suspendable
    private static <E> void addAll(Collection<E> c, Iterable<E> it) {
        for (E e : it)
            c.add(e);
    }
}
