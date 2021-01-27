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
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableRunnable;
import static co.paralleluniverse.fibers.TestsHelper.exec;
import org.junit.Test;

/**
 * Basic test
 *
 * @author Matthias Mann
 */
public class SuspendTest implements SuspendableRunnable {
    @Test
    public void testSuspend() {
        SuspendTest test = new SuspendTest();
        Fiber co = new Fiber((String)null, null, test);

        while (!exec(co))
            System.out.println("State=" + co.getState());

        System.out.println("State=" + co.getState());
    }

    @Override
    public void run() throws SuspendExecution {
        int i0 = 0, i1 = 1;
        for (int j = 0; j < 10; j++) {
            i1 = i1 + i0;
            i0 = i1 - i0;
            print("bla %d %d\n", i0, i1);
        }
    }

    private static void print(String fmt, Object... args) throws SuspendExecution {
        System.out.printf(fmt, args);
        Fiber.park();
    }
}
