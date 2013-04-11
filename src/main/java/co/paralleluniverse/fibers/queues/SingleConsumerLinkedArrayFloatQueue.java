/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedArrayFloatQueue extends SingleConsumerLinkedArrayWordQueue<Float> implements SingleConsumerFloatQueue<SingleConsumerLinkedArrayQueue.ElementPointer> {

    public void enq(float element) {
        super.enq(Float.floatToRawIntBits(element));
    }

    @Override
    public void enq(Float element) {
        enq(element.floatValue());
    }

    @Override
    public Float value(ElementPointer node) {
        return floatValue(node);
    }

    @Override
    public float floatValue(ElementPointer node) {
        return Float.intBitsToFloat(rawValue(node.n, node.i));
    }
}
