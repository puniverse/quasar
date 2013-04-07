/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.queues;

import co.paralleluniverse.concurrent.util.UtilUnsafe;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
abstract class SingleConsumerLinkedArrayQueue<E> extends SingleConsumerQueue<E, SingleConsumerLinkedArrayQueue.ElementPointer> {
    volatile Node head;
    int headIndex;
    volatile Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    volatile Node tail;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public SingleConsumerLinkedArrayQueue() {
        tail = head = newNode();
    }

    @Override
    public boolean allowRetainPointers() {
        return true;
    }

    abstract Node newNode();

    abstract boolean hasValue(Node n, int index);

    abstract boolean isDeleted(Node n, int index);

    abstract void markDeleted(Node n, int index);

    abstract int blockSize();

    @SuppressWarnings("empty-statement")
    @Override
    public void deq(ElementPointer ep) {
        final int blockSize = blockSize();
        int i = headIndex;
        Node n = head;
        for (;;) {
            int maxI = (n != ep.n ? blockSize - 1 : ep.i);
            for (; i <= maxI; i++)
                markDeleted(n, i);

            if (n != ep.n) {
                Node next = n.next; // can't be null because we're called by the consumer
                clearNext(n);
                clearPrev(next);

                n = next;
                i = 0;
            } else
                break;
        };

        if (head != n) // save the volatile write
            head = n;
        headIndex = ep.i + 1;
    }

    @Override
    public ElementPointer pk() {
        return current(new ElementPointer(head, headIndex));
    }

    @Override
    public ElementPointer succ(ElementPointer ep) {
        if (ep == null)
            return pk();

        ep.i++;
        if(current(ep) == null) {
            ep.i--; // restore ep
            return null;
        } else
            return ep;
    }

    @SuppressWarnings("empty-statement")
    private ElementPointer current(ElementPointer ep) {
        final int blockSize = blockSize();
        int i = ep.i;
        Node n = ep.n;
        for (;;) {
            if (i >= blockSize) {
                if (tail == n)
                    return null; // an enq following this will test the lock again

                while (n.next == null); // wait for next
                n = n.next;
                i = 0;
            } else if (hasValue(n, i)) {
                if (isDeleted(n, i))
                    i++;
                else {
                    ep.i = i;
                    ep.n = n;
                    return ep;
                }
            } else {
                assert n == tail;
                return null;
            }
        }
    }

    boolean isHead(ElementPointer ep) {
        return ep.n == head & ep.i == headIndex;
    }

    @SuppressWarnings("empty-statement")
    @Override
    public ElementPointer del(ElementPointer ep) {
        if (isHead(ep)) {
            deq(ep);
            return null;
        }

        markDeleted(ep.n, ep.i);
        return ep;
    }

    @Override
    public int size() {
        final int blockSize = blockSize();
        int count = 0;
        for (Node p = tail; p != null; p = p.prev) {
            for (int i = (p == head ? headIndex : 0); i < blockSize; i++) {
                if (p == tail && !hasValue(p, i))
                    break;
                if (!isDeleted(p, i))
                    count++;
            }
        }
        return count;
    }

    public int nodeCount() {
        int count = 0;
        for (Node p = tail; p != null; p = p.prev)
            count++;
        return count;
    }

    static abstract class Node {
        volatile Node next;
        volatile Node prev;
    }

    // mutable!
    public static class ElementPointer {
        Node n;
        int i;

        public ElementPointer(Node n, int i) {
            this.n = n;
            this.i = i;
        }
    }
    ////////////////////////////////////////////////////////////////////////
    static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long headOffset;
    private static final long tailOffset;
    private static final long nextOffset;
    private static final long prevOffset;
    private static final int base;
    private static final int shift;

    static {
        try {
            headOffset = unsafe.objectFieldOffset(SingleConsumerLinkedArrayQueue.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(SingleConsumerLinkedArrayQueue.class.getDeclaredField("tail"));
            nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
            prevOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("prev"));

            base = unsafe.arrayBaseOffset(Object[].class);
            int scale = unsafe.arrayIndexScale(Object[].class);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            shift = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    static boolean compareAndSetNext(Node node, Node expect, Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }

    private static void clearNext(Node node) {
        unsafe.putOrderedObject(node, nextOffset, null);
    }

    private static void clearPrev(Node node) {
        unsafe.putOrderedObject(node, prevOffset, null);
    }
}
