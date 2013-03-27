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

import java.util.Iterator;
import junit.framework.TestCase;
import org.junit.Test;

/**
 *
 * Test the {@link CoIterato} class
 * 
 * @author Matthias Mann
 */
public class CoIteratorTest extends TestCase {

    @Test
    public void testCoIterator() {
        Iterator<String> iter = new CoIterator<String>() {
            @Override
            public void run() throws SuspendExecution {
                for(int j=0 ; j<3 ; j++) {
                    produce("Hugo " + j);
                    produce("Test");
                    for(int i=1 ; i<10 ; i++) {
                        produce("Number " + i);
                    }
                    produce("Nix");
                }
            }
        };
        
        for(int j=0 ; j<3 ; j++) {
            assertEquals("Hugo " + j, iter.next());
            assertEquals("Test", iter.next());
            for(int i=1 ; i<10 ; i++) {
                assertEquals("Number " + i, iter.next());
            }
            assertEquals("Nix", iter.next());
        }
        
        assertFalse(iter.hasNext());
    }

}