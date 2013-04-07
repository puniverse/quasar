/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedArrayIntQueue extends SingleConsumerLinkedArrayWordQueue<Integer> implements SingleConsumerIntQueue<SingleConsumerLinkedArrayQueue.ElementPointer> {

    public void enq(int element) {
        super.enq(element);
    }

    @Override
    public void enq(Integer element) {
        enq(element.intValue());
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
