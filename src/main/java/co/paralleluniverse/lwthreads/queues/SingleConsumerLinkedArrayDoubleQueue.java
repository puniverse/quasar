/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedArrayDoubleQueue extends SingleConsumerLinkedArrayDWordQueue<Double> implements SingleConsumerDoubleQueue<SingleConsumerLinkedArrayQueue.ElementPointer> {

    public void enq(double element) {
        super.enq(Double.doubleToRawLongBits(element));
    }

    @Override
    public void enq(Double element) {
        enq(element.doubleValue());
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
