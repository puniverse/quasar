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
package de.matthiasmann.continuations;

import java.util.ArrayList;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Test the propagation of unhandled exceptions throw a suspendable call
 * 
 * @author Matthias Mann
 */
public class ThrowTest extends TestCase implements CoroutineProto {

    private ArrayList<String> results = new ArrayList<String>();
    
    public void coExecute() throws SuspendExecution {
        results.add("A");
        Coroutine.yield();
        try {
            results.add("C");
            Coroutine.yield();
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
        
        Coroutine co = new Coroutine(this);
        try {
            co.run();
            results.add("B");
            co.run();
            results.add("D");
            co.run();
            assertTrue(false);
        } catch (IllegalStateException es) {
            assertEquals("bla", es.getMessage());
            assertEquals(Coroutine.State.FINISHED, co.getState());
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
