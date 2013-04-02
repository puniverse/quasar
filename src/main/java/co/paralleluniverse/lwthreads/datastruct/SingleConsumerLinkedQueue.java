/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Objects;
import co.paralleluniverse.concurrent.util.UtilUnsafe;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
public abstract class SingleConsumerLinkedQueue<E> extends SingleConsumerQueue<E, SingleConsumerLinkedQueue.Node<E>> {
    public static final FlightRecorder RECORDER = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;
    volatile Node<E> head;
    Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    volatile Node<E> tail;

    @Override
    public void enq(E item) {
        if(item == null)
            throw new IllegalArgumentException("null values not allowed");
        enq(new Node(item));
    }

    @Override
    public E value(Node<E> node) {
        return node.value;
    }
    
    abstract void enq(final Node<E> node);

    @Override
    public abstract void deq(final Node<E> node);

    abstract boolean isHead(Node<E> node);

    @Override
    public abstract Node<E> pk();

    @SuppressWarnings("empty-statement")
    @Override
    public Node<E> succ(final Node<E> node) {
        record("succ", "queue: %s node: %s", this, node);
        if(node == null)
            return pk();
        if (tail == node) {
            record("succ", "return null");
            return null; // an enq following this will test the lock again
        }
        Node<E> succ;
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
        
        final Node<E> prev = node.prev;
        prev.next = null;
        final Node<E> t = tail;
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
            if (p.value != null)
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
        E value;
        volatile Node<E> next;
        volatile Node<E> prev;

        Node(E value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Node{" + "value: " + value + ", next: " + next + ", prev: " + Objects.systemToString(prev) + '}';
        }
    }
    ////////////////////////////////////////////////////////////////////////
    private static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long headOffset;
    private static final long tailOffset;
    private static final long nextOffset;
    private static final long prevOffset;
    private static final long valueOffset;

    static {
        try {
            headOffset = unsafe.objectFieldOffset(SingleConsumerLinkedQueue.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(SingleConsumerLinkedQueue.class.getDeclaredField("tail"));
            nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
            prevOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("prev"));
            valueOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("value"));

        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    /**
     * CAS head field. Used only by enq.
     */
    boolean compareAndSetHead(Node<E> update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    boolean compareAndSetTail(Node<E> expect, Node<E> update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS next field of a node.
     */
    static boolean compareAndSetNext(Node<?> node, Node<?> expect, Node<?> update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }

    static void clearNext(Node<?> node) {
        unsafe.putOrderedObject(node, nextOffset, null);
    }

    static void clearPrev(Node<?> node) {
        unsafe.putOrderedObject(node, prevOffset, null);
    }

    static void clearValue(Node<?> node) {
        unsafe.putOrderedObject(node, valueOffset, null);
    }

    ////////////////////////////
    protected boolean isRecording() {
        return RECORDER != null;
    }

    static void record(String method, String format) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerLinkedQueue", method, format, null));
    }

    static void record(String method, String format, Object arg1) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerLinkedQueue", method, format, new Object[]{arg1}));
    }

    static void record(String method, String format, Object arg1, Object arg2) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerLinkedQueue", method, format, new Object[]{arg1, arg2}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerLinkedQueue", method, format, new Object[]{arg1, arg2, arg3}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerLinkedQueue", method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerLinkedQueue", method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }
}
