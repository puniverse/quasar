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

import com.google.common.collect.Lists;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pron
 */
abstract class SingleConsumerLinkedQueue<E> extends SingleConsumerQueue<E> {
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
    public int capacity() {
        return -1;
    }

    @Override
    public E poll() {
        final Node<E> n = pk();
        if (n == null)
            return null;
        final E v = value(n);
        deq(n);
        return v;
    }

    @Override
    public E peek() {
        final Node<E> n = pk();
        return n != null ? value(n) : null;
    }

    abstract E value(Node<E> node);

    abstract Node newNode();

    boolean enq(final Node<E> node) {
        Node t;
        do {
            t = tail;
            node.prev = t;
        } while (!compareAndSetTail(t, node));
        if (t == null) // can't happen when DUMMY_NODE_ALGORITHM
            head = node;
        else
            t.next = node;
        return true;
    }

    @SuppressWarnings("empty-statement")
    void deq(final Node<E> node) {
        clearValue(node);

        if (DUMMY_NODE_ALGORITHM) {
            orderedSetHead(node); // head = node;
            clearPrev(node);
        } else {
            Node h = node.next;
            if (h == null) {
                orderedSetHead(null); // head = null; // Based on John M. Mellor-Crummey, "Concurrent Queues: Practical Fetch-and-phi Algorithms", 1987
                if (tail == node && compareAndSetTail(node, null)) { // a concurrent enq would either cause this to fail and wait for node.next, or have this succeed and then set tail and head
                    node.next = null;
                    return;
                }
                while ((h = node.next) == null);
            }
            orderedSetHead(h); // head = h;
            clearPrev(h); // h.prev = null;

            // clearNext(node); - we don't clear next so that iterator would work
            clearPrev(node);
        }
    }

    Node<E> pk() {
        if (DUMMY_NODE_ALGORITHM) {
            return succ(head);
        } else {
            if (tail == null)
                return null;

            for (;;) {
                Node h;
                if ((h = head) != null)
                    return h;
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
    Node<E> succ(final Node<E> node) {
        if (node == null)
            return pk();
        if (tail == node)
            return null; // an enq following this will test the lock again

        Node succ;
        while ((succ = node.next) == null); // wait for next
        return succ;
    }

    @SuppressWarnings("empty-statement")
    Node<E> del(Node<E> node) {
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
            while (node.next == null); // wait for next
            prev.next = node.next;
            node.next.prev = prev;
        }

        clearNext(node);
        clearPrev(node);
        return prev;
    }

    @Override
    public boolean isEmpty() {
        return pk() == null;
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

    @Override
    public List<E> snapshot() {
        final ArrayList<E> list = new ArrayList<E>();
        for (Node p = tail; p != null; p = p.prev) {
            if (DUMMY_NODE_ALGORITHM) {
                if (p.prev == null)
                    break;
            }
            list.add((E) value(p));
        }
        return Lists.reverse(list);
    }

    public static class Node<E> {
        volatile Node next;
        volatile Node prev;
    }

    @Override
    public QueueIterator<E> iterator() {
        return new LinkedQueueIterator();
    }

    class LinkedQueueIterator implements QueueIterator<E> {
        Node<E> n = null;

        @Override
        public boolean hasNext() {
            return succ(n) != null;
        }

        @Override
        public E value() {
            return SingleConsumerLinkedQueue.this.value(n);
        }

        @Override
        public void deq() {
            SingleConsumerLinkedQueue.this.deq(n);
        }

        @Override
        public void reset() {
            n = null;
        }

        @Override
        public E next() {
            n = succ(n);
            return value();
        }

        @Override
        public void remove() {
            n = del(n);
        }
    }
    ////////////////////////////////////////////////////////////////////////
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle NEXT;
    private static final VarHandle PREV;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            HEAD = l.findVarHandle(SingleConsumerLinkedQueue.class, "head", Node.class);
            TAIL = l.findVarHandle(SingleConsumerLinkedQueue.class, "tail", Node.class);
            NEXT = l.findVarHandle(Node.class, "next", Node.class);
            PREV = l.findVarHandle(Node.class, "prev", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    /**
     * CAS head field. Used only by enq.
     */
    boolean compareAndSetHead(Node update) {
        return HEAD.compareAndSet(this, null, update);
    }

    void orderedSetHead(Node value) {
        HEAD.setOpaque(this, value); // UNSAFE.putOrderedObject(this, headOffset, value);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    boolean compareAndSetTail(Node expect, Node update) {
        return TAIL.compareAndSet(this, expect, update);
    }

    static boolean compareAndSetNext(Node node, Node expect, Node update) {
        return NEXT.compareAndSet(node, expect, update);
    }

    private static void clearNext(Node node) {
        NEXT.setOpaque(node, null);
    }

    private static void clearPrev(Node node) {
        PREV.setOpaque(node, null);
    }
    
    abstract void clearValue(Node node);
        
//    static final Unsafe UNSAFE = UtilUnsafe.getUnsafe();
//    private static final long headOffset;
//    private static final long tailOffset;
//    private static final long nextOffset;
//    private static final long prevOffset;
//
//    static {
//        try {
//            headOffset = UNSAFE.objectFieldOffset(SingleConsumerLinkedQueue.class.getDeclaredField("head"));
//            tailOffset = UNSAFE.objectFieldOffset(SingleConsumerLinkedQueue.class.getDeclaredField("tail"));
//            nextOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
//            prevOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("prev"));
//        } catch (Exception ex) {
//            throw new Error(ex);
//        }
//    }
//
//    /**
//     * CAS head field. Used only by enq.
//     */
//    boolean compareAndSetHead(Node update) {
//        return UNSAFE.compareAndSwapObject(this, headOffset, null, update);
//    }
//
//    void orderedSetHead(Node value) {
//        UNSAFE.putOrderedObject(this, headOffset, value);
//    }
//
//    void volatileSetHead(Node value) {
//        UNSAFE.putObjectVolatile(this, headOffset, value);
//    }
//
//    /**
//     * CAS tail field. Used only by enq.
//     */
//    boolean compareAndSetTail(Node expect, Node update) {
//        return UNSAFE.compareAndSwapObject(this, tailOffset, expect, update);
//    }
//
//    /**
//     * CAS next field of a node.
//     */
//    static boolean compareAndSetNext(Node node, Node expect, Node update) {
//        return UNSAFE.compareAndSwapObject(node, nextOffset, expect, update);
//    }
//
//    static void clearNext(Node node) {
//        UNSAFE.putOrderedObject(node, nextOffset, null);
//    }
//
//    static void clearPrev(Node node) {
//        UNSAFE.putOrderedObject(node, prevOffset, null);
//    }
}
