/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedIntQueue extends SingleConsumerLinkedWordQueue<Integer> implements SingleConsumerIntQueue<SingleConsumerLinkedQueue.Node<Integer>> {
    @Override
    public boolean enq(int item) {
        return super.enq(item);
    }

    @Override
    public boolean enq(Integer item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        return enq(item.intValue());
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
