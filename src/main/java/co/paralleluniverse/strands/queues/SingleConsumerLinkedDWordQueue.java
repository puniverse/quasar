/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

import co.paralleluniverse.common.util.Objects;

/**
 *
 * @author pron
 */
abstract class SingleConsumerLinkedDWordQueue<E> extends SingleConsumerLinkedQueue<E> {
    @Override
    Node newNode() {
        return new DWordNode();
    }

    boolean enq(long item) {
        DWordNode node = new DWordNode();
        node.value = item;
        return enq(node);
    }

    long rawValue(Node node) {
        return ((DWordNode) node).value;
    }

    @Override
    void clearValue(Node node) {
    }

    static class DWordNode extends Node {
        long value;

        @Override
        public String toString() {
            return "Node{" + "value: " + value + ", next: " + next + ", prev: " + Objects.systemToString(prev) + '}';
        }
    }
}
