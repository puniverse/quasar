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
import java.util.NoSuchElementException;

/**
 *
 * @author pron
 */
abstract class SingleConsumerLinkedArrayQueue<E> extends SingleConsumerQueue<E> {
    volatile Node head;
    int headIndex;
    volatile Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    volatile Node tail;
    volatile int p016, p017, p018, p019, p020, p021, p022, p023, p024, p025, p026, p027, p028, p029, p030;
    int seed = (int) System.nanoTime();

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public SingleConsumerLinkedArrayQueue() {
        tail = head = newNode();
    }

    @Override
    public int capacity() {
        return -1;
    }

    abstract Node newNode();

    abstract boolean hasValue(Node n, int index);

    abstract boolean isDeleted(Node n, int index);

    abstract void markDeleted(Node n, int index);

    abstract int blockSize();

    abstract E value(Node n, int i);

    @SuppressWarnings("empty-statement")
    boolean prePeek() {
        // postconditions: head,headIndex point to the first element (i.e. empty/deleted slots are discarded)
        // returns whether an element exists
        final int blockSize = blockSize();
        Node n = head;
        int i = headIndex;
        boolean found = false;
        for (;;) {
            if (i >= blockSize) {
//                if (tail == n)
//                    break;
//                while (n.next == null); // wait for next
                Node next = n.next; // can't be null because we're called by the consumer
                if (next == null)
                    break; // we'll get it next time
                clearNext(n);
                clearPrev(next);

                n = next;
                i = 0;
            } else if (hasValue(n, i)) {
                if (isDeleted(n, i))
                    i++;
                else {
                    found = true;
                    break;
                }
            } else {
                // assert n == tail; - tail could have changed by now
                break;
            }
        }
        orderedSetHead(n);
        headIndex = i;
        return found;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public E peek() {
        return prePeek() ? value(head, headIndex) : null;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public E poll() {
        final E val = peek();
        if (val != null)
            deqHead();
        return val;
    }

    @Override
    public boolean isEmpty() {
        return peek() == null;
    }

    @SuppressWarnings("empty-statement")
    void deq(Node node, int index) {
        final int blockSize = blockSize();
        Node n = head;
        int i = headIndex;
        for (;;) {
            int maxI = (n != node ? blockSize - 1 : index);
            for (; i <= maxI; i++)
                markDeleted(n, i);

            if (n != node) {
                Node next = n.next; // can't be null because we're called by the consumer
                clearNext(n);
                clearPrev(next);

                n = next;
                i = 0;
            } else
                break;
        };

        // if (head != n) head = n; // save the volatile write
        orderedSetHead(n);
        headIndex = index + 1;
    }

    void deqHead() {
        markDeleted(head, headIndex);
        headIndex++;
    }

    boolean isHead(Node n, int i) {
        return n == head & i == headIndex;
    }

    boolean del(Node n, int i) {
        if (isHead(n, i)) {
            deq(n, i);
            return false;
        }

        markDeleted(n, i);
        return true;
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

    @Override
    public List<E> snapshot() {
        final int blockSize = blockSize();
        ArrayList<E> list = new ArrayList<E>();
        for (Node p = tail; p != null; p = p.prev) {
            for (int i = (p == head ? headIndex : 0); i < blockSize; i++) {
                if (p == tail && !hasValue(p, i))
                    break;
                if (hasValue(p, i) && !isDeleted(p, i))
                    list.add(value(p, i));
            }
        }
        return Lists.reverse(list);
    }

    public int nodeCount() {
        int count = 0;
        for (Node p = tail; p != null; p = p.prev)
            count++;
        return count;
    }

    void backoff() {
        int spins = 1 << 8;
        int r = seed;
        while (spins >= 0) {
            r ^= r << 1;
            r ^= r >>> 3;
            r ^= r << 10; // xorshift
            if (r >= 0)
                --spins;
        }
        seed = r;
    }

    static abstract class Node {
        volatile Node next;
        volatile Node prev;
    }

    @Override
    public QueueIterator<E> iterator() {
        return new LinkedArrayQueueIterator();
    }

    class LinkedArrayQueueIterator implements QueueIterator<E> {
        Node n;
        int i;
        private boolean hasNextCalled;

        LinkedArrayQueueIterator(Node n, int i) {
            this.n = n;
            this.i = i;
        }

        LinkedArrayQueueIterator() {
            this(null, -1);
        }

        @Override
        public boolean hasNext() {
            if (succ()) {
                hasNextCalled = true;
                return true;
            }
            return false;
        }

        @Override
        public E next() {
            preNext();
            return value();
        }

        final void preNext() {
            if (!hasNextCalled)
                if (!succ())
                    throw new NoSuchElementException();
            hasNextCalled = false;
        }

        @Override
        public void remove() {
            del(n, i);
        }

        @Override
        public E value() {
            return SingleConsumerLinkedArrayQueue.this.value(n, i);
        }

        @Override
        public void deq() {
            SingleConsumerLinkedArrayQueue.this.deq(n, i);
        }

        @Override
        public void reset() {
            n = null;
            i = -1;
            hasNextCalled = false;
        }

        @SuppressWarnings("empty-statement")
        boolean succ() {
            final int blockSize = blockSize();
            Node n = this.n != null ? this.n : head;
            int i = this.i + 1;
            for (;;) {
                if (i >= blockSize) {
                    if (tail == n)
                        return false;

                    while (n.next == null); // wait for next
                    n = n.next;
                    i = 0;
                } else if (hasValue(n, i)) {
                    if (isDeleted(n, i))
                        i++;
                    else {
                        this.i = i;
                        this.n = n;
                        return true;
                    }
                } else {
                    // assert n == tail; - tail could have changed by now
                    return false;
                }
            }
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
            HEAD = l.findVarHandle(SingleConsumerLinkedArrayQueue.class, "head", Node.class);
            TAIL = l.findVarHandle(SingleConsumerLinkedArrayQueue.class, "tail", Node.class);
            NEXT = l.findVarHandle(Node.class, "next", Node.class);
            PREV = l.findVarHandle(Node.class, "prev", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    boolean compareAndSetHead(Node update) {
        return HEAD.compareAndSet(this, null, update);
    }

    void orderedSetHead(Node value) {
        HEAD.setOpaque(this, value); // UNSAFE.putOrderedObject(this, headOffset, value);
    }

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
    
//    static final Unsafe UNSAFE = UtilUnsafe.getUnsafe();
//    private static final long headOffset;
//    private static final long tailOffset;
//    private static final long nextOffset;
//    private static final long prevOffset;
//
//    static {
//        try {
//            headOffset = UNSAFE.objectFieldOffset(SingleConsumerLinkedArrayQueue.class.getDeclaredField("head"));
//            tailOffset = UNSAFE.objectFieldOffset(SingleConsumerLinkedArrayQueue.class.getDeclaredField("tail"));
//            nextOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
//            prevOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("prev"));
//        } catch (Exception ex) {
//            throw new Error(ex);
//        }
//    }
//
//    boolean compareAndSetHead(Node update) {
//        return UNSAFE.compareAndSwapObject(this, headOffset, null, update);
//    }
//
//    void orderedSetHead(Node value) {
//        UNSAFE.putOrderedObject(this, headOffset, value);
//    }
//
//    boolean compareAndSetTail(Node expect, Node update) {
//        return UNSAFE.compareAndSwapObject(this, tailOffset, expect, update);
//    }
//
//    static boolean compareAndSetNext(Node node, Node expect, Node update) {
//        return UNSAFE.compareAndSwapObject(node, nextOffset, expect, update);
//    }
//
//    private static void clearNext(Node node) {
//        UNSAFE.putOrderedObject(node, nextOffset, null);
//    }
//
//    private static void clearPrev(Node node) {
//        UNSAFE.putOrderedObject(node, prevOffset, null);
//    }
}
