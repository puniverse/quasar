/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedArrayIntQueue extends SingleConsumerLinkedArrayWordQueue<Integer> implements SingleConsumerIntQueue<SingleConsumerLinkedArrayQueue.ElementPointer> {

    @Override
    public boolean enq(int element) {
        return super.enq(element);
    }

    @Override
    public boolean enq(Integer element) {
        return enq(element.intValue());
    }

    @Override
    public Integer value(ElementPointer node) {
        return intValue(node);
    }

    @Override
    public int intValue(ElementPointer node) {
        return rawValue(node.n, node.i);
    }
    
}
