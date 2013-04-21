/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayLongQueue extends SingleConsumerArrayDWordQueue<Long> implements SingleConsumerLongQueue<Integer> {
    public SingleConsumerArrayLongQueue(int capacity) {
        super(capacity);
    }

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

    public long longValue(int index) {
        return rawValue(index);
    }

    @Override
    public Long value(int index) {
        return longValue(index);
    }

    @Override
    public long longValue(Integer node) {
        return longValue(node.intValue());
    }
}
