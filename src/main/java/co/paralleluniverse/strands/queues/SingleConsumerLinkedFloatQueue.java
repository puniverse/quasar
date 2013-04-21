/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedFloatQueue extends SingleConsumerLinkedWordQueue<Float> implements SingleConsumerFloatQueue<SingleConsumerLinkedQueue.Node<Float>> {
    @Override
    public boolean enq(float item) {
        return super.enq(Float.floatToRawIntBits(item));
    }

    @Override
    public boolean enq(Float item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        return enq(item.floatValue());
    }

    @Override
    public float floatValue(Node<Float> node) {
        return Float.intBitsToFloat(rawValue(node));
    }

    @Override
    public Float value(Node<Float> node) {
        return floatValue(node);
    }
}
