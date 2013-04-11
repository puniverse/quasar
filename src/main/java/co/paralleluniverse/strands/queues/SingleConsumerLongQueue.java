/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public interface SingleConsumerLongQueue<Node> {
    void enq(long item);
    long longValue(Node node);
}
