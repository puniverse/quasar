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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedObjectQueue<E> extends SingleConsumerLinkedQueue<E> {
    @Override
    Node<E> newNode() {
        return new ObjectNode<E>();
    }

    @Override
    public boolean enq(E item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        ObjectNode node = new ObjectNode();
        node.value = item;
        return enq(node);
    }

    @Override
    E value(Node<E> node) {
        return ((ObjectNode<E>) node).value;
    }

    static class ObjectNode<E> extends Node<E> {
        E value;

        @Override
        public String toString() {
            return "Node{" + "value: " + value + ", next: " + next + ", prev: " + Objects.systemToString(prev) + '}';
        }
    }

    private static final VarHandle VALUE;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(ObjectNode.class, "value", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    void clearValue(Node node) {
        VALUE.setOpaque(node, null); // UNSAFE.putOrderedObject(node, valueOffset, null);
    }
    
//    private static final long valueOffset;
//
//    static {
//        try {
//            valueOffset = UNSAFE.objectFieldOffset(ObjectNode.class.getDeclaredField("value"));
//        } catch (Exception ex) {
//            throw new Error(ex);
//        }
//    }
//
//    @Override
//    void clearValue(Node node) {
//        UNSAFE.putOrderedObject(node, valueOffset, null);
//    }
}
