/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
public abstract class SingleConsumerArrayDWordQueue<E> extends SingleConsumerArrayPrimitiveQueue<E> {
    public static SingleConsumerArrayDWordQueue<Long> newLongQueue(int size) {
        return new SingleConsumerArrayDWordQueue<Long>(size) {
            @Override
            public void enq(Long item) {
                if (item == null)
                    throw new IllegalArgumentException("null values not allowed");
                enq(item.longValue());
            }

            @Override
            public Long value(int index) {
                return longValue(index);
            }
        };
    }

    public static SingleConsumerArrayDWordQueue<Double> newDoubleQueue(int size) {
        return new SingleConsumerArrayDWordQueue<Double>(size) {
            @Override
            public void enq(Double item) {
                if (item == null)
                    throw new IllegalArgumentException("null values not allowed");
                enq(item.doubleValue());
            }

            @Override
            public Double value(int index) {
                return doubleValue(index);
            }
        };
    }
    private final long[] array;

    public SingleConsumerArrayDWordQueue(int size) {
        this.array = new long[size];
    }

    public long longValue(int index) {
        return array[index];
    }

    public double doubleValue(int index) {
        return Double.longBitsToDouble(array[index]);
    }

    @Override
    int arrayLength() {
        return array.length;
    }

    public void enq(long item) {
        final int i = preEnq();
        set(i, item);
        postEnq(i);
    }

    public void enq(double item) {
        enq(Double.doubleToRawLongBits(item));
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
