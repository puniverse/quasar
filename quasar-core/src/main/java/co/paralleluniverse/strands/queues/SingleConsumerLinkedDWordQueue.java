/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.strands.queues;

import co.paralleluniverse.common.util.Objects;

/**
 *
 * @author pron
 */
abstract class SingleConsumerLinkedDWordQueue<E> extends SingleConsumerLinkedPrimitiveQueue<E> {
    @Override
    Node newNode() {
        return new DWordNode();
    }

    public boolean enqRaw(long item) {
        DWordNode node = new DWordNode();
        node.value = item;
        return enq(node);
    }

    long rawValue(Node node) {
        return ((DWordNode) node).value;
    }

    @Override
    void clearValue(Node node) {
    }

    static class DWordNode extends Node {
        long value;

        @Override
        public String toString() {
            return "Node{" + "value: " + value + ", next: " + next + ", prev: " + Objects.systemToString(prev) + '}';
        }
    }
}
