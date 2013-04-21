/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayIntQueue extends SingleConsumerArrayWordQueue<Integer> implements SingleConsumerIntQueue<Integer> {
    public SingleConsumerArrayIntQueue(int capcity) {
        super(capcity);
    }

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

    public int intValue(int index) {
        return rawValue(index);
    }

    @Override
    public Integer value(int index) {
        return intValue(index);
    }

    @Override
    public int intValue(Integer node) {
        return intValue(node.intValue());
    }
}
