/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public interface SingleConsumerFloatQueue<Node> {
    void enq(float item);
    float floatValue(Node node);
}
