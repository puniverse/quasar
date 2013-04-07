/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.queues;

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
    int blockSize() {
        return BLOCK_SIZE;
    }
    
    @Override
    public void enq(E item) {
        for (;;) {
            final Node t = tail;
            for (int i = 0; i < BLOCK_SIZE; i++) {
                if (get(t, i) == null && compareAndSetElement(t, i, null, item))
                    return;
            }

            Node n = new Node();
            n.prev = t;
            if (compareAndSetTail(t, n))
                t.next = n;
        }
    }

    @Override
    public E value(ElementPointer ep) {
        // called after hasValue so no need for a volatile read
        return (E)((ObjectNode)ep.n).array[ep.i];
    }

    @Override
    boolean hasValue(Node n, int index) {
        return get(n, index) != null;
    }
    
    @Override
    boolean isDeleted(Node n, int index) {
        // called after hasValue so no need for volatile read
        return ((ObjectNode)n).array[index] != TOMBSTONE;
    }
    
    @Override
    void markDeleted(Node n, int index) {
        ((ObjectNode)n).array[index] = TOMBSTONE;
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
            base = unsafe.arrayBaseOffset(Object[].class);
            int scale = unsafe.arrayIndexScale(Object[].class);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            shift = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private static boolean compareAndSetElement(Node n, int i, Object expect, Object update) {
        return unsafe.compareAndSwapObject(((ObjectNode) n).array, byteOffset(i), expect, update);
    }

    private static void lazySet(Node n, int i, Object value) {
        unsafe.putOrderedObject(((ObjectNode) n).array, byteOffset(i), value);
    }

    private static Object get(Node n, int i) {
        return unsafe.getObjectVolatile(((ObjectNode) n).array, byteOffset(i));
    }

    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }
}
