/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
abstract class SingleConsumerLinkedArrayDWordQueue<E> extends SingleConsumerLinkedArrayPrimitiveQueue<E> {
    public static final int BLOCK_SIZE = 5;

    @Override
    int blockSize() {
        return BLOCK_SIZE;
    }

    boolean enq(long item) {
        ElementPointer ep = preEnq();
        ((WordNode) ep.n).array[ep.i] = item;
        postEnq(ep.n, ep.i);
        return true;
    }

    long rawValue(Node n, int i) {
        return ((WordNode) n).array[i];
    }

    @Override
    PrimitiveNode newNode() {
        return new WordNode();
    }

    private static class WordNode extends PrimitiveNode {
        final long[] array = new long[BLOCK_SIZE];
    }
}
