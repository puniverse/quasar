/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayFloatQueue extends SingleConsumerArrayWordQueue<Float> implements SingleConsumerFloatQueue<Integer> {
    public SingleConsumerArrayFloatQueue(int size) {
        super(size);
    }

    @Override
    public void enq(float item) {
        super.enq(Float.floatToRawIntBits(item));
    }

    @Override
    public void enq(Float item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        enq(item.floatValue());
    }

    public float floatValue(int index) {
        return Float.intBitsToFloat(rawValue(index));
    }

    @Override
    public Float value(int index) {
        return floatValue(index);
    }

    @Override
    public float floatValue(Integer node) {
        return floatValue(node.intValue());
    }
}
