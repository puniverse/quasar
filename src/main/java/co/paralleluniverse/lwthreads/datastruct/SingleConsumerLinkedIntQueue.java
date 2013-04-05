/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedIntQueue extends SingleConsumerLinkedWordQueue<Integer> implements SingleConsumerIntQueue<SingleConsumerLinkedQueue.Node<Integer>> {
    @Override
    public void enq(int item) {
        super.enq(item);
    }

    @Override
    public void enq(Integer item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        enq(item.intValue());
    }

    @Override
    public int intValue(Node<Integer> node) {
        return rawValue(node);
    }

    @Override
    public Integer value(Node<Integer> node) {
        return intValue(node);
    }
}
