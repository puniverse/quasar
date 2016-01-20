/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.instrument.live.basic;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.instrument.live.LiveInstrumentationTest;
import co.paralleluniverse.strands.SuspendableCallable;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author circlespainter
 */
public final class MethodHandleTest extends LiveInstrumentationTest {
    private final MethodHandle mhm, mhm1;

    public MethodHandleTest() throws NoSuchMethodException, IllegalAccessException {
        final Method m = F.class.getDeclaredMethod("m", String.class);
        m.setAccessible(true);
        mhm = MethodHandles.lookup().unreflect(m);
        final Method m1 = F.class.getDeclaredMethod("m1", String.class);
        m1.setAccessible(true);
        mhm1 = MethodHandles.lookup().unreflect(m1);
    }

    private final class F implements SuspendableCallable<Integer> {
        @Override
        // @Suspendable
        public final Integer run() throws InterruptedException {
            final String s = "ciao";
            System.err.println("Enter run(), calling m(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final int ret;
            try {
                ret = (Integer) mhm.invoke(this, s);
            } catch (final Throwable e) {
                throw new RuntimeException(e);
            }
            System.err.println("Exit run(), called m(" + s + ")");
            assertThat(s, equalTo("ciao"));
            return ret;
        }

        /** @noinspection unused*/
        // @Suspendable
        private int m(String s) {
            System.err.println("Enter m(" + s + "), calling m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            final int ret;
            try {
                ret = (Integer) mhm1.invoke(this, s);
            } catch (final Throwable e) {
                throw new RuntimeException(e);
            }
            System.err.println("Exit m(" + s + "), called m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            return ret;
        }

        /** @noinspection unused*/
        // @Suspendable
        private int m1(String s) {
            System.err.println("Enter m1(" + s + "), sleeping");
            assertThat(s, equalTo("ciao"));
            try {
                Fiber.sleep(10);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.err.println("Exit m1(" + s + ")");
            assertThat(s, equalTo("ciao"));
            return -1;
        }
    }

    @Test public final void test() {
        final Fiber<Integer> f1 = new Fiber<>(new F()).start();
        try {
            assertThat(f1.get(), equalTo(-1));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        final Fiber<Integer> f2 = new Fiber<>(new F()).start();
        try {
            assertThat(f2.get(), equalTo(-1));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        assertThat(LiveInstrumentation.fetchRunCount(), equalTo(1L));
    }
}
