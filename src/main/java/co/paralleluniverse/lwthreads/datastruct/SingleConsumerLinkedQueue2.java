/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedQueue2<E> extends SingleConsumerLinkedQueue<E> {
    public SingleConsumerLinkedQueue2() {
        tail = head = new Node(null);
    }

    @Override
    void enq(final Node<E> node) {
        record("enq", "queue: %s node: %s", this, node);
        for (;;) {
            Node t = tail;
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return;
            }
        }
    }

    @SuppressWarnings("empty-statement")
    @Override
    public void deq(final Node<E> node) {
        record("deq", "queue: %s node: %s", this, node);
        clearValue(node);

        head = node;
        node.prev = null;
    }

    @Override
    public Node<E> pk() {
        return succ(head);
    }

//    @Override
//    public Node<E> succ(Node<E> node) {
//        Node s = node.next;
//        if (s != null)
//            return s;
//
//        for (Node t = tail; t != null && t != node; t = t.prev)
//            if (t.prev == node)
//                return t;
//        throw new AssertionError();
//    }
    @Override
    boolean isHead(Node<E> node) {
        return node.prev == head;
    }
}
