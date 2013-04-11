/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedArrayLongQueue extends SingleConsumerLinkedArrayDWordQueue<Long> implements SingleConsumerLongQueue<SingleConsumerLinkedArrayQueue.ElementPointer> {

    public void enq(long element) {
        super.enq(element);
    }

    @Override
    public void enq(Long element) {
        enq(element.longValue());
    }

    @Override
    public Long value(ElementPointer node) {
        return longValue(node);
    }

    @Override
    public long longValue(ElementPointer node) {
        return rawValue(node.n, node.i);
    }
}
