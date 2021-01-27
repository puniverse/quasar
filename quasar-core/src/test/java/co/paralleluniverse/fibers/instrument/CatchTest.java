/*
 * Copyright (c) 2008, Matthias Mann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static co.paralleluniverse.fibers.TestsHelper.exec;
import static org.junit.Assert.*;

/**
 * Check that a generic catch all does not affect the suspension of a method
 *
 * @author Matthias Mann
 */
public class CatchTest {
    private ArrayList<String> results = new ArrayList<String>();

    class Runnable1 implements SuspendableRunnable {
        int cnt = 0;

        private void throwOnSecondCall() throws SuspendExecution {
            results.add("cnt=" + cnt);
            Fiber.park();
            if (++cnt >= 2) {
                throw new IllegalStateException("called second time");
            }
            results.add("not thrown");
        }

        @Override
        public void run() throws SuspendExecution {
            results.add("A");
            Fiber.park();
            try {
                results.add("C");
                Fiber.park();
                throwOnSecondCall();
                suspendableMethod();
                results.add("never reached");
            } catch (Throwable ex) {
                results.add(ex.getMessage());
            }
            results.add("H");
        }

        private void suspendableMethod() throws SuspendExecution {
            Fiber.park();
            throwOnSecondCall();
        }
    }

    @Test
    public void testCatch() {
        results.clear();

        try {
            Fiber co = new Fiber((String) null, null, new Runnable1());
            exec(co);
            results.add("B");
            exec(co);
            results.add("D");
            exec(co);
            results.add("E");
            exec(co);
            results.add("F");
            exec(co);
            results.add("G");
            exec(co);
            results.add("I");
        } finally {
            System.out.println(results);
        }

        assertEquals(13, results.size());
        assertEquals(Arrays.asList(
                "A",
                "B",
                "C",
                "D",
                "cnt=0",
                "E",
                "not thrown",
                "F",
                "cnt=1",
                "G",
                "called second time",
                "H",
                "I"), results);
    }

    class Callable1 implements SuspendableCallable<Integer> {
        @Override
        public Integer run() throws SuspendExecution {
            try {
                results.add("A");
                Fiber.park();
                results.add("C");
                Fiber.park();
                results.add("E");
                return 3;
            } catch (Exception ex) {
                //System.out.println("EX: " + ex);
                throw new RuntimeException(ex);
            }
        }
    }

    @Test
    public void testCatch2() {
        results.clear();

        try {
            Fiber co = new Fiber((String) null, null, new Callable1());
            exec(co);
            results.add("B");
            exec(co);
            results.add("D");
            exec(co);
        } finally {
            System.out.println(results);
        }

        assertEquals(5, results.size());
        assertEquals(Arrays.asList(
                "A",
                "B",
                "C",
                "D",
                "E"), results);
    }

    // See #211
    @Test
    public void testParkBeforeCatch() throws ExecutionException, InterruptedException {
        new Fiber(new SuspendableRunnable() {
            @Override
            public final void run() throws SuspendExecution, InterruptedException {
                catchSuspendable();
            }
        }).start().join();
    }

    @Suspendable
    private static void catchSuspendable() throws InterruptedException {
        try {
            Fiber.park(10, TimeUnit.MILLISECONDS);
        } catch (final SuspendExecution s) {
            throw new AssertionError(s);
        }
    }
}
