/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedLongQueue extends SingleConsumerLinkedDWordQueue<Long> implements SingleConsumerLongQueue<SingleConsumerLinkedQueue.Node<Long>> {
    @Override
    public boolean enq(long item) {
        return super.enq(item);
    }

    @Override
    public boolean enq(Long item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        return enq(item.longValue());
    }

    @Override
    public long longValue(Node<Long> node) {
        return rawValue(node);
    }

    @Override
    public Long value(Node<Long> node) {
        return longValue(node);
    }
}
