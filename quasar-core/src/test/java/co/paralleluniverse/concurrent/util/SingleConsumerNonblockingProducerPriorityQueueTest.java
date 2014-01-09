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
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.BlockingQueue;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author pron
 */
public class SingleConsumerNonblockingProducerPriorityQueueTest {
    public SingleConsumerNonblockingProducerPriorityQueueTest() {
    }
    BlockingQueue<Foo> q;

    @Before
    public void setUp() {
        q = new SingleConsumerNonblockingProducerQueue<Foo>(new ConcurrentSkipListPriorityQueue<Foo>());
    }

    @Test
    public void simpleProiorityTest() throws Exception {
        int index = 0;
        q.put(new Foo(5, index++));
        q.put(new Foo(10, index++));
        q.put(new Foo(3, index++));
        q.put(new Foo(7, index++));
        q.put(new Foo(20, index++));
        
        assertThat(q.size(), is(5));
        assertThat(q.poll().priority, is(3));
        assertThat(q.poll().priority, is(5));
        assertThat(q.poll().priority, is(7));
        assertThat(q.poll().priority, is(10));
        assertThat(q.poll().priority, is(20));
        assertThat(q.isEmpty(), is(true));
    }
    
    static class Foo implements Comparable<Foo> {
        final int priority;
        final int index;

        public Foo(int priority, int index) {
            this.priority = priority;
            this.index = index;
        }

        @Override
        public int compareTo(Foo o) {
            return this.priority - o.priority;
        }
    }
}
