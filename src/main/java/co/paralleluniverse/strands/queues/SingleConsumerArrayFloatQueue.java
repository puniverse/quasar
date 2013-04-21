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
    public SingleConsumerArrayFloatQueue(int capacity) {
        super(capacity);
    }

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
