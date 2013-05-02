/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

/**
 *
 * @author pron
 */
abstract class SingleConsumerLinkedArrayDWordQueue<E> extends SingleConsumerLinkedArrayPrimitiveQueue<E> {
    public static final int BLOCK_SIZE = 5;

    @Override
    int blockSize() {
        return BLOCK_SIZE;
    }

    boolean enq(long item) {
        ElementPointer ep = preEnq();
        ((WordNode) ep.n).array[ep.i] = item;
        postEnq(ep.n, ep.i);
        return true;
    }

    long rawValue(Node n, int i) {
        return ((WordNode) n).array[i];
    }

    @Override
    PrimitiveNode newNode() {
        return new WordNode();
    }

    private static class WordNode extends PrimitiveNode {
        final long[] array = new long[BLOCK_SIZE];
    }
}
