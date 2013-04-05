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
public abstract class SingleConsumerLinkedWordQueue<E> extends SingleConsumerLinkedQueue<E> {
    public static SingleConsumerLinkedWordQueue<Integer> newIntegerQueue() {
        return new SingleConsumerLinkedWordQueue<Integer>() {
            @Override
            public void enq(Integer item) {
                if (item == null)
                    throw new IllegalArgumentException("null values not allowed");
                enq(item.intValue());
            }

            @Override
            public Integer value(Node<Integer> node) {
                return intValue(node);
            }
        };
    }

    public static SingleConsumerLinkedWordQueue<Float> newFloatQueue() {
        return new SingleConsumerLinkedWordQueue<Float>() {
            @Override
            public void enq(Float item) {
                if (item == null)
                    throw new IllegalArgumentException("null values not allowed");
                enq(item.floatValue());
            }

            @Override
            public Float value(Node<Float> node) {
                return floatValue(node);
            }
        };
    }

    @Override
    Node newNode() {
        return new WordNode();
    }

    public void enq(int item) {
        WordNode node = new WordNode();
        node.value = item;
        enq(node);
    }

    public void enq(float item) {
        enq(Float.floatToRawIntBits(item));
    }

    public int intValue(Node<Integer> node) {
        return ((WordNode) node).value;
    }

    public float floatValue(Node<Float> node) {
        return Float.intBitsToFloat(((WordNode) node).value);
    }

    @Override
    void clearValue(Node node) {
    }

    static class WordNode extends Node {
        int value;

        @Override
        public String toString() {
            return "Node{" + "value: " + value + ", next: " + next + ", prev: " + Objects.systemToString(prev) + '}';
        }
    }
}
