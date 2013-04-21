/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
abstract class SingleConsumerArrayWordQueue<E> extends SingleConsumerArrayPrimitiveQueue<E> {
    private final int[] array;

    public SingleConsumerArrayWordQueue(int capacity) {
        super(nextPowerOfTwo(capacity));
        this.array = new int[nextPowerOfTwo(capacity)];
    }

    int rawValue(int index) {
        return array[index];
    }

    @Override
    int arrayLength() {
        return array.length;
    }

    boolean enq(int item) {
        final long i = preEnq();
        if(i < 0)
            return false;
        array[(int) i & mask] = item; // no need for volatile semantics because postEnq does a volatile write (cas) which is then read in await value
        postEnq(i);
        return true;
    }

    @Override
    void copyValue(int to, int from) {
        array[to] = array[from];
    }
}
