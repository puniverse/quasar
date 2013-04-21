/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayDoubleQueue extends SingleConsumerArrayDWordQueue<Double> implements SingleConsumerDoubleQueue<Integer> {
    public SingleConsumerArrayDoubleQueue(int capacity) {
        super(capacity);
    }

    @Override
    public boolean enq(double item) {
        return super.enq(Double.doubleToRawLongBits(item));
    }

    @Override
    public boolean enq(Double item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        return enq(item.doubleValue());
    }

    public double doubleValue(int index) {
        return Double.longBitsToDouble(rawValue(index));
    }

    @Override
    public Double value(int index) {
        return doubleValue(index);
    }

    @Override
    public double doubleValue(Integer node) {
        return doubleValue(node.intValue());
    }
}
