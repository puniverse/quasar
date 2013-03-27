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
import java.util.Iterator;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Check that a generic catch all does not affect the suspendtion of a method
 * 
 * @author Matthias Mann
 */
public class CatchTest extends TestCase implements CoroutineProto {

    private ArrayList<String> results = new ArrayList<String>();
    
    int cnt = 0;
    private void throwOnSecondCall() throws SuspendExecution {
        results.add("cnt=" + cnt);
        Coroutine.yield();
        if(++cnt >= 2) {
            throw new IllegalStateException("called second time");
        }
        results.add("not thrown");
    }
    
    public void coExecute() throws SuspendExecution {
        results.add("A");
        Coroutine.yield();
        try {
            results.add("C");
            Coroutine.yield();
            throwOnSecondCall();
            Coroutine.yield();
            throwOnSecondCall();
            results.add("never reached");
        } catch(Throwable ex) {
            results.add(ex.getMessage());
        }
        results.add("H");
    }

    @Test
    public void testCatch() {
        results.clear();
        
        try {
            Coroutine co = new Coroutine(this);
            co.run();
            results.add("B");
            co.run();
            results.add("D");
            co.run();
            results.add("E");
            co.run();
            results.add("F");
            co.run();
            results.add("G");
            co.run();
            results.add("I");
        } finally {
            System.out.println(results);
        }
        
        assertEquals(13, results.size());
        Iterator<String> iter = results.iterator();
        assertEquals("A", iter.next());
        assertEquals("B", iter.next());
        assertEquals("C", iter.next());
        assertEquals("D", iter.next());
        assertEquals("cnt=0", iter.next());
        assertEquals("E", iter.next());
        assertEquals("not thrown", iter.next());
        assertEquals("F", iter.next());
        assertEquals("cnt=1", iter.next());
        assertEquals("G", iter.next());
        assertEquals("called second time", iter.next());
        assertEquals("H", iter.next());
        assertEquals("I", iter.next());
    }
}
