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
import java.util.ArrayList;
import static org.junit.Assert.*;
import org.junit.Test;


/**
 * Test correct execution of a finally statement
 * 
 * @author Matthias Mann
 */
public class FinallyTest implements SuspendableRunnable {

    private ArrayList<String> results = new ArrayList<String>();
    
    @Override
    public void run() throws SuspendExecution {
        results.add("A");
        Fiber.park();
        try {
            results.add("C");
            Fiber.park();
            results.add("E");
        } finally {
            results.add("F");
        }
        results.add("G");
        Fiber.park();
        results.add("I");
    }

    @Test
    public void testFinally() {
        results.clear();
        
        try {
            Fiber co = new Fiber((String)null, null, this);
            exec(co);
            results.add("B");
            exec(co);
            results.add("D");
            exec(co);
            results.add("H");
            exec(co);
        } finally {
            System.out.println(results);
        }
        
        assertEquals(9, results.size());
        assertEquals("A", results.get(0));
        assertEquals("B", results.get(1));
        assertEquals("C", results.get(2));
        assertEquals("D", results.get(3));
        assertEquals("E", results.get(4));
        assertEquals("F", results.get(5));
        assertEquals("G", results.get(6));
        assertEquals("H", results.get(7));
        assertEquals("I", results.get(8));
    }
}
