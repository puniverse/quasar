/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedArrayDoubleQueue extends SingleConsumerLinkedArrayDWordQueue<Double> implements SingleConsumerDoubleQueue<SingleConsumerLinkedArrayQueue.ElementPointer> {

    @Override
    public boolean enq(double element) {
        return super.enq(Double.doubleToRawLongBits(element));
    }

    @Override
    public boolean enq(Double element) {
        return enq(element.doubleValue());
    }

    @Override
    public Double value(ElementPointer node) {
        return doubleValue(node);
    }

    @Override
    public double doubleValue(ElementPointer node) {
        return Double.longBitsToDouble(rawValue(node.n, node.i));
    }
}
