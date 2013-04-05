/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
abstract class SingleConsumerArrayWordQueue<E> extends SingleConsumerArrayPrimitiveQueue<E> {
    private final int[] array;

    public SingleConsumerArrayWordQueue(int size) {
        this.array = new int[size];
    }

    public int rawValue(int index) {
        return array[index];
    }

    @Override
    int arrayLength() {
        return array.length;
    }

    void enq(int item) {
        final int i = preEnq();
        set(i, item);
        postEnq(i);
    }

    @Override
    void copyValue(int to, int from) {
        lazySet(to, array[from]);
    }
    private static final int base;
    private static final int shift;

    static {
        try {
            base = unsafe.arrayBaseOffset(int[].class);
            int scale = unsafe.arrayIndexScale(int[].class);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            shift = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }

    private void set(int i, int value) {
        unsafe.putIntVolatile(array, byteOffset(i), value);
    }

    private void lazySet(int i, int value) {
        unsafe.putOrderedInt(array, byteOffset(i), value);
    }
}
