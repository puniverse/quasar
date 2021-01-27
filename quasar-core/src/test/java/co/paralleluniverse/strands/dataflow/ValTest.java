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
package co.paralleluniverse.strands.dataflow;

import co.paralleluniverse.common.test.TestUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

/**
 *
 * @author pron
 */
public class ValTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;

    public ValTest() {
    }

    @Test(expected = IllegalStateException.class)
    public void whenValIsSetTwiceThenThrowException() throws Exception {
        final Val<String> val = new Val<>();

        val.set("hello");
        val.set("goodbye");
    }

    @Test
    public void testThreadWaiter() throws Exception {
        final Val<String> val = new Val<>();

        final AtomicReference<String> res = new AtomicReference<>();

        final Thread t1 = new Thread(Strand.toRunnable(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                try {
                    final String v = val.get();
                    res.set(v);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }));
        t1.start();

        Thread.sleep(100);

        val.set("yes!");

        t1.join();

        assertThat(res.get(), equalTo("yes!"));
        assertThat(val.get(), equalTo("yes!"));
    }

    @Test
    public void testFiberWaiter() throws Exception {
        final Val<String> val = new Val<>();

        final Fiber<String> f1 = new Fiber<String>(new SuspendableCallable<String>() {
            @Override
            public String run() throws SuspendExecution, InterruptedException {
                final String v = val.get();
                return v;
            }
        }).start();

        Thread.sleep(100);

        val.set("yes!");

        f1.join();

        assertThat(f1.get(), equalTo("yes!"));
        assertThat(val.get(), equalTo("yes!"));
    }

    @Test
    public void testThreadAndFiberWaiters() throws Exception {
        final Val<String> val = new Val<>();

        final AtomicReference<String> res = new AtomicReference<>();

        final Fiber<String> f1 = new Fiber<String>(new SuspendableCallable<String>() {
            @Override
            public String run() throws SuspendExecution, InterruptedException {
                final String v = val.get();
                return v;
            }
        }).start();

        final Thread t1 = new Thread(Strand.toRunnable(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                try {
                    final String v = val.get();
                    res.set(v);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }));
        t1.start();

        Thread.sleep(100);

        val.set("yes!");

        t1.join();
        f1.join();

        assertThat(f1.get(), equalTo("yes!"));
        assertThat(res.get(), equalTo("yes!"));
        assertThat(val.get(), equalTo("yes!"));
    }

    @Test
    public void complexTest1() throws Exception {
        final Val<Integer> val1 = new Val<>();
        final Val<Integer> val2 = new Val<>();

        final AtomicReference<Integer> res = new AtomicReference<>();

        final Fiber<Integer> f1 = new Fiber<Integer>(new SuspendableCallable<Integer>() {
            @Override
            public Integer run() throws SuspendExecution, InterruptedException {
                return val1.get() + val2.get();
            }
        }).start();

        final Thread t1 = new Thread(Strand.toRunnable(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                try {
                    res.set(val1.get() * val2.get());
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }));
        t1.start();

        final Fiber<Integer> f2 = new Fiber<Integer>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                val2.set(5);
            }
        }).start();

        Thread.sleep(100);

        val1.set(8);

        int myRes = val1.get() - val2.get();
        t1.join();
        f1.join();

        assertThat(f1.get(), equalTo(13));
        assertThat(res.get(), equalTo(40));
        assertThat(myRes, is(3));

        f2.join();
    }

    @Test
    public void complexTest2() throws Exception {
        final Val<Integer> val1 = new Val<>();
        final Val<Integer> val2 = new Val<>();
        final Val<Integer> val3 = new Val<>();
        final Val<Integer> val4 = new Val<>();

        final AtomicReference<Integer> res = new AtomicReference<>();

        final Strand f1 = new Fiber<Integer>(new SuspendableRunnable() {
            @Override
            public void run() throws InterruptedException, SuspendExecution {
                val2.set(val1.get() + 1); // 2
            }
        }).start();

        final Strand t1 = Strand.of(new Thread(Strand.toRunnable(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution {
                try {
                    val3.set(val1.get() + val2.get()); // 3
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }))).start();

        final Strand f2 = new Fiber<Integer>(new SuspendableRunnable() {
            @Override
            public void run() throws InterruptedException, SuspendExecution {
                val4.set(val2.get() + val3.get()); // 5
            }
        }).start();

        Thread.sleep(100);

        val1.set(1);

        int myRes = val4.get();
        assertThat(myRes, is(5));

        t1.join();
        f1.join();
        f2.join();
    }
}
