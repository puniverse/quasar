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

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import static co.paralleluniverse.fibers.TestsHelper.exec;
import co.paralleluniverse.strands.Strand;
import java.util.ArrayList;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test the propagation of unhandled exceptions throw a suspendable call
 * 
 * @author Matthias Mann
 */
public class ThrowTest implements SuspendableRunnable {
    private static Strand.UncaughtExceptionHandler previousUEH;

    private final ArrayList<String> results = new ArrayList<>();
    
    @BeforeClass
    public static void setupClass() {
        previousUEH = Fiber.getDefaultUncaughtExceptionHandler();
        Fiber.setDefaultUncaughtExceptionHandler(new Strand.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Strand s, Throwable e) {
                Exceptions.rethrow(e);
            }
        });
    }

    @AfterClass
    public static void afterClass() {
        // Restore
        Fiber.setDefaultUncaughtExceptionHandler(previousUEH);
    }

    @Override
    public void run() throws SuspendExecution {
        results.add("A");
        Fiber.park();
        try {
            results.add("C");
            Fiber.park();
            if("".length() == 0) {
                throw new IllegalStateException("bla");
            }
            results.add("E");
        } finally {
            results.add("F");
        }
        results.add("G");
    }

    @Test
    public void testThrow() {
        results.clear();

        Fiber co = new Fiber((String)null, null, this);
        try {
            exec(co);
            results.add("B");
            exec(co);
            results.add("D");
            exec(co);
            assertTrue(false);
        } catch (IllegalStateException es) {
            assertEquals("bla", es.getMessage());
            //assertEquals(LightweightThread.State.FINISHED, co.getState());
        } finally {
            System.out.println(results);
        }

        assertEquals(5, results.size());
        assertEquals("A", results.get(0));
        assertEquals("B", results.get(1));
        assertEquals("C", results.get(2));
        assertEquals("D", results.get(3));
        assertEquals("F", results.get(4));
    }
}
