/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedArrayFloatQueue extends SingleConsumerLinkedArrayWordQueue<Float> implements SingleConsumerFloatQueue<SingleConsumerLinkedArrayQueue.ElementPointer> {

    @Override
    public boolean enq(float element) {
        return super.enq(Float.floatToRawIntBits(element));
    }

    @Override
    public boolean enq(Float element) {
        return enq(element.floatValue());
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
