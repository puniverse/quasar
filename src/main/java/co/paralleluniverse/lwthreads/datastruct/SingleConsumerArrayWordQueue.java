/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.datastruct;

/**
 *
 * @author pron
 */
public abstract class SingleConsumerArrayWordQueue<E> extends SingleConsumerArrayPrimitiveQueue<E> {
    public static SingleConsumerArrayWordQueue<Integer> newIntegerQueue(int size) {
        return new SingleConsumerArrayWordQueue<Integer>(size) {
            @Override
            public void enq(Integer item) {
                if (item == null)
                    throw new IllegalArgumentException("null values not allowed");
                enq(item.intValue());
            }

            @Override
            public Integer value(int index) {
                return intValue(index);
            }
        };
    }

    public static SingleConsumerArrayWordQueue<Float> newFloatQueue(int size) {
        return new SingleConsumerArrayWordQueue<Float>(size) {
            @Override
            public void enq(Float item) {
                if (item == null)
                    throw new IllegalArgumentException("null values not allowed");
                enq(item.floatValue());
            }

            @Override
            public Float value(int index) {
                return floatValue(index);
            }
        };
    }
    private final int[] array;

    public SingleConsumerArrayWordQueue(int size) {
        this.array = new int[size];
    }

    public int intValue(int index) {
        return array[index];
    }

    public float floatValue(int index) {
        return Float.intBitsToFloat(array[index]);
    }

    @Override
    int arrayLength() {
        return array.length;
    }

    public void enq(int item) {
        final int i = preEnq();
        set(i, item);
        postEnq(i);
    }

    public void enq(float item) {
        enq(Float.floatToRawIntBits(item));
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
