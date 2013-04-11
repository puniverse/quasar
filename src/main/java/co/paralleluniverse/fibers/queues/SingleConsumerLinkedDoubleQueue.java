/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedDoubleQueue extends SingleConsumerLinkedDWordQueue<Double> implements SingleConsumerDoubleQueue<SingleConsumerLinkedQueue.Node<Double>> {
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

    @Override
    public double doubleValue(Node<Double> node) {
        return Double.longBitsToDouble(rawValue(node));
    }

    @Override
    public Double value(Node<Double> node) {
        return doubleValue(node);
    }
}
