/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.queues;

import co.paralleluniverse.common.util.Objects;

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
    public void enq(E item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        ObjectNode node = new ObjectNode();
        node.value = item;
        enq(node);
    }

    @Override
    public E value(Node<E> node) {
        return ((ObjectNode<E>) node).value;
    }

    static class ObjectNode<E> extends Node<E> {
        E value;

        @Override
        public String toString() {
            return "Node{" + "value: " + value + ", next: " + next + ", prev: " + Objects.systemToString(prev) + '}';
        }
    }
    private static final long valueOffset;

    static {
        try {
            valueOffset = unsafe.objectFieldOffset(ObjectNode.class.getDeclaredField("value"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @Override
    void clearValue(Node node) {
        unsafe.putOrderedObject(node, valueOffset, null);
    }
}
