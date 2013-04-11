/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.queues;

/**
 *
 * @author pron
 */
public interface SingleConsumerIntQueue<Node> {
    void enq(int item);
    int intValue(Node node);
}
