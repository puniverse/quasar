/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

import co.paralleluniverse.concurrent.util.UtilUnsafe;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
abstract class SingleConsumerLinkedQueue<E> extends SingleConsumerQueue<E, SingleConsumerLinkedQueue.Node<E>> {
    private static final boolean DUMMY_NODE_ALGORITHM = false;
    volatile Node head;
    volatile Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    volatile Node tail;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public SingleConsumerLinkedQueue() {
        if (DUMMY_NODE_ALGORITHM) {
            tail = head = newNode();
        }
    }

    @Override
    public boolean allowRetainPointers() {
        return true;
    }

    abstract Node newNode();

    @Override
    public boolean isFull() {
        return false;
    }
    
    void enq(final Node<E> node) {
        record("enq", "queue: %s node: %s", this, node);
        for (;;) {
            final Node t = tail;
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                if (t == null) { // can't happen when DUMMY_NODE_ALGORITHM
                    head = node;
                    record("enq", "set head");
                } else
                    t.next = node;
                break;
            }
        }
    }

    @SuppressWarnings("empty-statement")
    @Override
    public void deq(final Node<E> node) {
        record("deq", "queue: %s node: %s", this, node);

        clearValue(node);

        if (DUMMY_NODE_ALGORITHM) {
            head = node;
            node.prev = null;
        } else {
            Node h = node.next;
            if (h == null) {
                head = null; // Based on John M. Mellor-Crummey, "Concurrent Queues: Practical Fetch-and-ø Algorithms", 1987
                if (tail == node && compareAndSetTail(node, null)) { // a concurrent enq would either cause this to fail and wait for node.next, or have this succeed and then set tail and head
                    record("deq", "set tail to null");
                    node.next = null;
                    return;
                }
                while ((h = node.next) == null);
            }
            head = h;
            h.prev = null;

            record("deq", "set head to %s", h);

            // clearNext(node); - we don't clear next so that iterator would work
            clearPrev(node);
        }
    }

    @Override
    public Node<E> pk() {
        if (DUMMY_NODE_ALGORITHM) {
            return succ(head);
        } else {
            if (tail == null) {
                record("peek", "return null");
                return null;
            }

            for (;;) {
                Node h;
                if ((h = head) != null) {
                    record("peek", "return %s", h);
                    return h;
                }
            }
        }
    }

    boolean isHead(Node node) {
        if (DUMMY_NODE_ALGORITHM)
            return node.prev == head;
        else
            return node.prev == null;
    }

    @SuppressWarnings("empty-statement")
    @Override
    public Node<E> succ(final Node<E> node) {
        if (node == null)
            return pk();
        if (tail == node) {
            record("succ", "return null");
            return null; // an enq following this will test the lock again
        }
        Node succ;
        while ((succ = node.next) == null); // wait for next
        record("succ", "return %s", succ);
        return succ;
    }

    @SuppressWarnings("empty-statement")
    @Override
    public Node<E> del(Node<E> node) {
        if (isHead(node)) {
            deq(node);
            return null;
        }

        clearValue(node);

        final Node prev = node.prev;
        prev.next = null;
        final Node t = tail;
        if (t != node || !compareAndSetTail(t, node.prev)) {
            // neither head nor tail
            prev.next = node.next;
            while (node.next == null); // wait for next
            node.next.prev = prev;
        }

        clearNext(node);
        clearPrev(node);
        return prev;
    }

    @Override
    public int size() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (DUMMY_NODE_ALGORITHM) {
                if (p.prev == null)
                    break;
            }
            n++;
        }
        return n;

//        int count = 0;
//        Node<E> n = null;
//        for (;;) {
//            if (tail == null)
//                return 0;
//            if (tail == n)
//                return count;
//
//            if (n == null)
//                n = head;
//            else {
//                int i;
//                for (i = 0; i < 10; i++) {
//                    Node<E> next = n.next;
//                    if (next != null) {
//                        n = next;
//                        break;
//                    }
//                }
//                if (i == 10)
//                    n = null; // retry
//
//            }
//            if (n != null)
//                count++;
//        }
    }

    public static class Node<E> {
        volatile Node next;
        volatile Node prev;
    }
    ////////////////////////////////////////////////////////////////////////
    static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long headOffset;
    private static final long tailOffset;
    private static final long nextOffset;
    private static final long prevOffset;

    static {
        try {
            headOffset = unsafe.objectFieldOffset(SingleConsumerLinkedQueue.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(SingleConsumerLinkedQueue.class.getDeclaredField("tail"));
            nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
            prevOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("prev"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    /**
     * CAS head field. Used only by enq.
     */
    boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS next field of a node.
     */
    static boolean compareAndSetNext(Node node, Node expect, Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }

    static void clearNext(Node node) {
        unsafe.putOrderedObject(node, nextOffset, null);
    }

    static void clearPrev(Node node) {
        unsafe.putOrderedObject(node, prevOffset, null);
    }

    abstract void clearValue(Node node);
}
