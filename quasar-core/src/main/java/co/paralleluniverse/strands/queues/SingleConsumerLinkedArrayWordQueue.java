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

/**
 *
 * @author pron
 */
abstract class SingleConsumerLinkedArrayWordQueue<E> extends SingleConsumerLinkedArrayPrimitiveQueue<E> {
    public static final int BLOCK_SIZE = 8;
    private static final long MASK = 0xFFFFFFFF;

    @Override
    int blockSize() {
        return BLOCK_SIZE;
    }

    @Override
    void enqRaw(Node n, int i, long item) {
        ((WordNode) n).array[i] = (int) item;
    }

    @Override
    long getRaw(Node n, int i) {
        return rawValue(n, i) & MASK;
    }

    int rawValue(Node n, int i) {
        return ((WordNode) n).array[i];
    }

    @Override
    Node newNode() {
        return new WordNode();
    }

    private static class WordNode extends PrimitiveNode {
        final int[] array = new int[BLOCK_SIZE];
    }
}
