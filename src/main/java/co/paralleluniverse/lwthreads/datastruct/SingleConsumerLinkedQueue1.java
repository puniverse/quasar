/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
public class SingleConsumerLinkedQueue1<E> extends SingleConsumerLinkedQueue<E> {
    // This implementation uses next refs only (w/o prev refs), which are
    // set only after the tail has been CASed. When a next ref is null, we spin in waiting for it to be set.
    // The prerequisite for this approach is that the thread doing the enq is very unlikely to be descheduled by the OS, so that the wait for the next
    // ref will be short. This is a reasonable assumption when running in an FJ pool with fewer threads than CPU cores (and few/nonbusy threads outside the pool)
    // waker invariant: at any point, there is at most one task waking up others. it's either the task releasing a write, or the last task to release a read
    @Override
    void enq(final Node<E> node) {
        record("enq", "queue: %s node: %s", this, node);
        for (;;) {
            final Node<E> t = tail;
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                if (t == null) {
                    head = node;
                    record("enq", "set head");
                } else
                    t.next = node;
                break;
            }
        }
    }

    @SuppressWarnings("empty-statement")
    @Override
    public void deq(final Node<E> node) {
        record("deq", "queue: %s node: %s", this, node);
        Node<E> h = node.next;
        if (h == null) {
            head = null; // Based on John M. Mellor-Crummey, "Concurrent Queues: Practical Fetch-and-ø Algorithms", 1987
            if (tail == node && compareAndSetTail(node, null)) { // a concurrent enq would either cause this to fail and wait for node.next, or have this succeed and then set tail and head
                record("deq", "set tail to null");
                node.next = null;
                return;
            }
            while ((h = node.next) == null);
        }
        head = h;
        record("deq", "set head to %s", h);
        node.next = null;
        node.prev = null;
    }

    @Override
    public Node<E> peek() {
        if (tail == null) {
            record("peek", "return null");
            return null;
        }

        for (;;) {
            Node<E> h;
            if ((h = head) != null) {
                record("peek", "return %s", h);
                return h;
            }
        }
    }
    
    @Override
    boolean isHead(Node<E> node) {
        return node.prev == null;
    }
}
