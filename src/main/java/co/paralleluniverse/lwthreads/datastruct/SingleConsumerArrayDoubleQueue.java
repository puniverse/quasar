/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayDoubleQueue extends SingleConsumerArrayDWordQueue<Double> implements SingleConsumerDoubleQueue<Integer> {
    public SingleConsumerArrayDoubleQueue(int size) {
        super(size);
    }

    @Override
    public void enq(double item) {
        super.enq(Double.doubleToRawLongBits(item));
    }

    @Override
    public void enq(Double item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        enq(item.doubleValue());
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
