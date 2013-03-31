/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
public interface SingleConsumerQueue<E, Node> {
    public E value(Node node);
    void enq(E element);
    Node peek();
    Node succ(Node node);
    void deq(Node node);
    void del(Node node);
    int size();
}
