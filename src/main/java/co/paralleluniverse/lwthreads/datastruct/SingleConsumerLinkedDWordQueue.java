/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

import co.paralleluniverse.common.util.Objects;

/**
 *
 * @author pron
 */
public abstract class SingleConsumerLinkedDWordQueue<E> extends SingleConsumerLinkedQueue<E> {
    public static SingleConsumerLinkedDWordQueue<Long> newLongQueue() {
        return new SingleConsumerLinkedDWordQueue<Long>() {
            @Override
            public void enq(Long item) {
                if (item == null)
                    throw new IllegalArgumentException("null values not allowed");
                enq(item.longValue());
            }

            @Override
            public Long value(Node<Long> node) {
                return longValue(node);
            }
        };
    }

    public static SingleConsumerLinkedDWordQueue<Double> newDoubleQueue() {
        return new SingleConsumerLinkedDWordQueue<Double>() {
            @Override
            public void enq(Double item) {
                if (item == null)
                    throw new IllegalArgumentException("null values not allowed");
                enq(item.doubleValue());
            }

            @Override
            public Double value(Node<Double> node) {
                return doubleValue(node);
            }
        };
    }

    @Override
    Node newNode() {
        return new DWordNode();
    }

    public void enq(long item) {
        DWordNode node = new DWordNode();
        node.value = item;
        enq(node);
    }

    public void enq(double item) {
        enq(Double.doubleToRawLongBits(item));
    }

    public long longValue(Node<Long> node) {
        return ((DWordNode) node).value;
    }

    public double doubleValue(Node<Double> node) {
        return Double.longBitsToDouble(((DWordNode) node).value);
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
