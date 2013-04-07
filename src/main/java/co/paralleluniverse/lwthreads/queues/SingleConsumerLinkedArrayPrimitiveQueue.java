/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.queues;

/**
 *
 * @author pron
 */
public abstract class SingleConsumerLinkedArrayPrimitiveQueue<E> extends SingleConsumerLinkedArrayQueue<E> {
    final ElementPointer preEnq() {
        final int blockSize = blockSize();
        for (;;) {
            final PrimitiveNode t = (PrimitiveNode) tail;
            final int i = t.tailIndex;

            if (i < blockSize) {
                if (compareAndSetTailIndex(t, i, i + 1))
                    return new ElementPointer(t, i);
            } else {
                Node n = newNode();
                n.prev = t;
                if (compareAndSetTail(t, n))
                    t.next = n;
            }
        }
    }

    @SuppressWarnings("empty-statement")
    final void postEnq(Node n, int i) {
        while (!compareAndSetMaxReadIndex(n, i, i+1))
            ;
    }

    @Override
    boolean hasValue(Node n, int index) {
        return index < ((PrimitiveNode)n).maxReadIndex;
    }

    @Override
    boolean isDeleted(Node n, int index) {
        return getBit(((PrimitiveNode) n).deleted, index);
    }

    @Override
    void markDeleted(Node n1, int index) {
        assert index <= blockSize();
        final PrimitiveNode n = (PrimitiveNode) n1;
        n.deleted = setBit(n.deleted, index);
    }

    static abstract class PrimitiveNode extends Node {
        volatile int tailIndex;
        volatile int maxReadIndex;
        short deleted;
    }

    private static short setBit(short bits, int index) {
        return (short) (bits | (1 << index));
    }

    private static boolean getBit(short bits, int index) {
        return (bits >>> index & 1) != 0;
    }
    private static final long tailIndexOffset;
    private static final long maxReadIndexOffset;

    static {
        try {
            tailIndexOffset = unsafe.objectFieldOffset(PrimitiveNode.class.getDeclaredField("tailIndex"));
            maxReadIndexOffset = unsafe.objectFieldOffset(PrimitiveNode.class.getDeclaredField("maxReadIndex"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private boolean compareAndSetTailIndex(Node n, int expect, int update) {
        return unsafe.compareAndSwapInt((PrimitiveNode) n, tailIndexOffset, expect, update);
    }

    private boolean compareAndSetMaxReadIndex(Node n, int expect, int update) {
        return unsafe.compareAndSwapInt((PrimitiveNode) n, maxReadIndexOffset, expect, update);
    }
}
