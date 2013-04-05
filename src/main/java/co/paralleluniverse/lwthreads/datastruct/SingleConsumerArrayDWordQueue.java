/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
abstract class SingleConsumerArrayDWordQueue<E> extends SingleConsumerArrayPrimitiveQueue<E> {
    private final long[] array;

    public SingleConsumerArrayDWordQueue(int size) {
        this.array = new long[size];
    }

    public long rawValue(int index) {
        return array[index];
    }

    @Override
    int arrayLength() {
        return array.length;
    }

    void enq(long item) {
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
            base = unsafe.arrayBaseOffset(long[].class);
            int scale = unsafe.arrayIndexScale(long[].class);
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

    private void set(int i, long value) {
        unsafe.putLongVolatile(array, byteOffset(i), value);
    }

    private void lazySet(int i, long value) {
        unsafe.putOrderedLong(array, byteOffset(i), value);
    }
}
