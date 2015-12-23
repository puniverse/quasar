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
package co.paralleluniverse.fibers.instrument.auto;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Test;
import org.junit.Ignore;

import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author circlespainter
 */
public class AutoSingleUninstrCallSiteArgsTest {
    static class F implements SuspendableCallable<Integer> {
        @Override
        public Integer run() throws SuspendExecution, InterruptedException {
            System.err.println (
                "Enter run(), calling m(" +
                    "b:false, by: 1, c: 'a', s: 2, i: 3, l: 10, f: 1.3, d: 1.4, s1: 'ciao', s2, 'hello'" +
                ")"
            );
            int ret = m(false, (byte) 1, 'a', (short) 2, 3, 10, 1.3F, 1.4D, "ciao", "hello");
            System.err.println (
                "Exit run(), called m(" +
                    "b:false, by: 1, c: 'a', s: 2, i: 3, l: 10, f: 1.3, d: 1.4, s1: 'ciao', s2, 'hello'" +
                ")"
            );
            System.err.println("Exit run()");
            return ret;
        }

        // @Suspendable
        public int m(boolean b, byte by, char c, short s, int i, long l, float f, double d, String s1, String s2) {
            System.err.println (
                "Enter m(" +
                    "b:" + b + ", by:" + by + ", c:" + c + ", s:" + s + ", i:" + i + ", l:" + l + ", " +
                    "f:" + f + ", d:" + d + ", s1:" + s1 + ", s2:" + s2 +
                "), calling m1(...)"
            );
            assertThat(b, equalTo(false));
            assertThat(by, equalTo((byte) 1));
            assertThat(c, equalTo('a'));
            assertThat(s, equalTo((short) 2));
            assertThat(i, equalTo(3));
            assertThat(l, equalTo(10L));
            assertThat(f, equalTo(1.3F));
            assertThat(d, equalTo(1.4D));
            assertThat(s1, equalTo("ciao"));
            assertThat(s2, equalTo("hello"));
            int ret = m1(b, by, c, s, i, l, f, d, s1, s2);
            System.err.println (
                "Exit m(" +
                    "b:" + b + ", by:" + by + ", c:" + c + ", s:" + s + ", i:" + i + ", l:" + l + ", " +
                    "f:" + f + ", d:" + d + ", s1:" + s1 + ", s2:" + s2 +
                "), called m1(...)"
            );
            assertThat(b, equalTo(false));
            assertThat(by, equalTo((byte) 1));
            assertThat(c, equalTo('a'));
            assertThat(s, equalTo((short) 2));
            assertThat(i, equalTo(3));
            assertThat(l, equalTo(10L));
            assertThat(f, equalTo(1.3F));
            assertThat(d, equalTo(1.4D));
            assertThat(s1, equalTo("ciao"));
            assertThat(s2, equalTo("hello"));
            System.err.println("Exit m()");
            return ret;
        }

        @Suspendable
        public int m1(boolean b, byte by, char c, short s, int i, long l, float f, double d, String s1, String s2) {
            System.err.println (
                "Enter m1(" +
                    "b:" + b + ", by:" + by + ", c:" + c + ", s:" + s + ", i:" + i + ", l:" + l + ", " +
                    "f:" + f + ", d:" + d + ", s1:" + s1 + ", s2:" + s2 +
                "), sleeping"
            );
            assertThat(b, equalTo(false));
            assertThat(by, equalTo((byte) 1));
            assertThat(c, equalTo('a'));
            assertThat(s, equalTo((short) 2));
            assertThat(i, equalTo(3));
            assertThat(l, equalTo(10L));
            assertThat(f, equalTo(1.3F));
            assertThat(d, equalTo(1.4D));
            assertThat(s1, equalTo("ciao"));
            assertThat(s2, equalTo("hello"));
            try {
                Fiber.sleep(10);
            } catch (final InterruptedException | SuspendExecution e) {
                throw new RuntimeException(e);
            }
            System.err.println (
                "Exit m1(" +
                    "b:" + b + ", by:" + by + ", c:" + c + ", s:" + s + ", i:" + i + ", l:" + l + ", " +
                    "f:" + f + ", d:" + d + ", s1:" + s1 + ", s2:" + s2 +
                ")"
            );
            assertThat(b, equalTo(false));
            assertThat(by, equalTo((byte) 1));
            assertThat(c, equalTo('a'));
            assertThat(s, equalTo((short) 2));
            assertThat(i, equalTo(3));
            assertThat(l, equalTo(10L));
            assertThat(f, equalTo(1.3F));
            assertThat(d, equalTo(1.4D));
            assertThat(s1, equalTo("ciao"));
            assertThat(s2, equalTo("hello"));
            return -1;
        }
    }

    @Test public void test() {
        final Fiber f1 = new Fiber(new F()).start();
        try {
            assertThat(f1.get(), equalTo(-1));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        final Fiber f2 = new Fiber(new F()).start();
        try {
            assertThat(f2.get(), equalTo(-1));
        } catch (final ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
