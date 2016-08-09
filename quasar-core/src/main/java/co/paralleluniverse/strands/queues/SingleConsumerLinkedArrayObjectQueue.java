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
import java.util.Arrays;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedArrayObjectQueue<E> extends SingleConsumerLinkedArrayQueue<E> {
    public static final int BLOCK_SIZE = 10;
    private static final Object TOMBSTONE = new Object();

    @Override
    Node newNode() {
        return new ObjectNode();
    }

    @Override
    int blockSize() {
        return BLOCK_SIZE;
    }

    @Override
    public boolean enq(E item) {
        Node n = null;
        for (;;) {
            final Node t = tail;
            for (int i = 0; i < BLOCK_SIZE; i++) {
                if (get(t, i) == null) {
                    if (compareAndSetElement(t, i, null, item))
                        return true;
                    // backoff();
                }
            }

            if (n == null) {
                n = newNode();
                set(n, 0, item);
            }
            n.prev = t;
            if (compareAndSetTail(t, n)) {
                t.next = n;
                return true;
            }
            else
                backoff();
        }
    }

    @Override
    E value(Node n, int i) {
        // called after hasValue so no need for a volatile read
        return (E) ((ObjectNode) n).array[i];
    }

    @Override
    boolean hasValue(Node n, int index) {
        return get(n, index) != null;
    }

    @Override
    boolean isDeleted(Node n, int index) {
        // called after hasValue so no need for volatile read
        return ((ObjectNode) n).array[index] == TOMBSTONE;
    }

    @Override
    void markDeleted(Node n, int index) {
        ((ObjectNode) n).array[index] = TOMBSTONE;
    }

    static class ObjectNode extends Node {
        final Object[] array = new Object[BLOCK_SIZE];

        @Override
        public String toString() {
            return "Node{" + "array: " + Arrays.toString(array) + ", next: " + next + ", prev: " + Objects.systemToString(prev) + '}';
        }
    }
    private static final int base;
    private static final int shift;

    static {
        try {
            base = UNSAFE.arrayBaseOffset(Object[].class);
            int scale = UNSAFE.arrayIndexScale(Object[].class);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            shift = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private static boolean compareAndSetElement(Node n, int i, Object expect, Object update) {
        return UNSAFE.compareAndSwapObject(((ObjectNode) n).array, byteOffset(i), expect, update);
    }

    private static void lazySet(Node n, int i, Object value) {
        UNSAFE.putOrderedObject(((ObjectNode) n).array, byteOffset(i), value);
    }

    private static Object get(Node n, int i) {
        return UNSAFE.getObjectVolatile(((ObjectNode) n).array, byteOffset(i));
    }

    private static void set(Node n, int i, Object x) {
        ((ObjectNode) n).array[i] = x;
    }

    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }
}
