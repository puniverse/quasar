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

    public SingleConsumerArrayWordQueue(int size) {
        super(nextPowerOfTwo(size));
        this.array = new int[nextPowerOfTwo(size)];
    }

    int rawValue(int index) {
        return array[index];
    }

    @Override
    int arrayLength() {
        return array.length;
    }

    void enq(int item) {
        final int i = preEnq();
        array[i] = item; // no need for volatile semantics because postEnq does a volatile write (cas) which is then read in await value
        postEnq(i);
    }

    @Override
    void copyValue(int to, int from) {
        array[to] = array[from];
    }
}
