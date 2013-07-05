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

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Objects;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 *
 * @author pron
 */
public abstract class SingleConsumerQueue<E, Node> extends AbstractCollection<E> implements Iterable<E>, Queue<E>, BasicSingleConsumerQueue<E> {
    public static final FlightRecorder RECORDER = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;

    @Override
    public abstract boolean enq(E element);

    public abstract E value(Node node);

    public abstract Node pk();

    public abstract Node succ(Node node);

    public abstract void deq(Node node);

    public abstract Node del(Node node);

    public abstract boolean allowRetainPointers();

    @Override
    public abstract int size();

    @Override
    public abstract int capacity();

    @Override
    public boolean hasNext() {
        return !isEmpty();
    }
    
    @Override
    public boolean isEmpty() {
        return pk() == null;
    }

    @Override
    public boolean add(E e) {
        enq(e);
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(E e) {
        return enq(e);
    }

    @Override
    public E remove() {
        final E val = poll();
        if (val == null)
            throw new NoSuchElementException();
        return val;
    }

    @Override
    public E poll() {
        final Node n = pk();
        if (n == null)
            return null;
        final E val = value(n);
        deq(n);
        return val;
    }

    @Override
    public E element() {
        final E val = peek();
        if (val == null)
            throw new NoSuchElementException();
        return val;
    }

    @Override
    public E peek() {
        final Node n = pk();
        if (n == null)
            return null;
        return value(n);
    }

    @Override
    public Iterator<E> iterator() {
        return new QueueIterator();
    }

    public void resetIterator(Iterator<E> iter) {
        ((QueueIterator) iter).n = null;
    }

    private class QueueIterator implements Iterator<E> {
        private Node n;
        private boolean hasNextCalled;

        @Override
        public boolean hasNext() {
            Node next = succ(n);
            hasNextCalled = true;
            if(next != null) {// in case we want to remove the last element, we're not getting rid of the node just yet
                n = next;
                return true;
            } else
                return false;
        }

        @Override
        public E next() {
            if (!hasNextCalled)
                n = succ(n);
            hasNextCalled = false;
            return value(n);
        }

        @Override
        public void remove() {
            n = del(n);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Objects.systemToString(this));
        sb.append('[');
        for (Iterator<E> it = iterator(); it.hasNext();)
            sb.append(it.next()).append(", ");
        if (sb.length() > 1)
            sb.delete(sb.length() - 2, sb.length());
        sb.append(']');
        return sb.toString();
    }

    ////////////////////////////
    boolean isRecording() {
        return RECORDER != null;
    }

    static void record(String method, String format) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerQueue", method, format, null));
    }

    static void record(String method, String format, Object arg1) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerQueue", method, format, new Object[]{arg1}));
    }

    static void record(String method, String format, Object arg1, Object arg2) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerQueue", method, format, new Object[]{arg1, arg2}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerQueue", method, format, new Object[]{arg1, arg2, arg3}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerQueue", method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("SingleConsumerQueue", method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }
}
