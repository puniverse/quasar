/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayIntQueue extends SingleConsumerArrayWordQueue<Integer> implements SingleConsumerIntQueue<Integer> {
    public SingleConsumerArrayIntQueue(int size) {
        super(size);
    }

    @Override
    public void enq(int item) {
        super.enq(item);
    }

    @Override
    public void enq(Integer item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        enq(item.intValue());
    }

    public int intValue(int index) {
        return rawValue(index);
    }

    @Override
    public Integer value(int index) {
        return intValue(index);
    }

    @Override
    public int intValue(Integer node) {
        return intValue(node.intValue());
    }
}
