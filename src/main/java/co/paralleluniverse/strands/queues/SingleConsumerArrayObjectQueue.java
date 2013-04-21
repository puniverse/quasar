/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
public class SingleConsumerArrayObjectQueue<E> extends SingleConsumerArrayQueue<E> {
    private final Object[] array;

    public SingleConsumerArrayObjectQueue(int size) {
        super(nextPowerOfTwo(size));
        this.array = new Object[nextPowerOfTwo(size)];
    }

    @Override
    public E value(int index) {
        return (E) array[index];
    }

    @Override
    int arrayLength() {
        return array.length;
    }

    @Override
    public boolean enq(E item) {
        if (item == null)
            throw new IllegalArgumentException("null values not allowed");
        final long i = preEnq();
        if(i < 0)
            return false;
        set((int) i & mask, item);
        return true;
    }

    @SuppressWarnings("empty-statement")
    @Override
    void awaitValue(long i) {
        while (get((int) i & mask) == null); // volatile read
    }
    
    @Override
    void clearValue(long index) {
        array[(int) index & mask] = null; //orderedSet(index, null);
    }

    @Override
    void copyValue(int to, int from) {
        array[to] = array[from]; // orderedSet(to, array[from]);
    }
    
    private static final int base;
    private static final int shift;

    static {
        try {
            base = unsafe.arrayBaseOffset(Object[].class);
            int scale = unsafe.arrayIndexScale(Object[].class);
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

    private void set(int i, Object value) {
        unsafe.putObjectVolatile(array, byteOffset(i), value);
    }

    private void orderedSet(int i, Object value) {
        unsafe.putOrderedObject(array, byteOffset(i), value);
    }

    private Object get(int i) {
        return unsafe.getObjectVolatile(array, byteOffset(i));
    }
}
