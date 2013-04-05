/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
public interface SingleConsumerDoubleQueue<Node> {
    void enq(double item);
    double doubleValue(Node node);
}
