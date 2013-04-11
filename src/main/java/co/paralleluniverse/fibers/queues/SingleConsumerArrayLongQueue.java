/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayLongQueue extends SingleConsumerArrayDWordQueue<Long> implements SingleConsumerLongQueue<Integer> {
    public SingleConsumerArrayLongQueue(int size) {
        super(size);
    }

    @Override
    public void enq(long item) {
        super.enq(item);
    }

    @Override
    public void enq(Long item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        enq(item.longValue());
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
