/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.queues;

/**
 *
 * @author pron
 */
abstract class SingleConsumerArrayPrimitiveQueue<E> extends SingleConsumerArrayQueue<E> {
    private volatile Object p001, p002, p003, p004, p005, p006, p007, p008, p009, p010, p011, p012, p013, p014, p015;
    volatile long maxReadIndex;

    public SingleConsumerArrayPrimitiveQueue(int capacity) {
        super(capacity);
    }

    @Override
    void clearValue(long index) {
    }

    @SuppressWarnings("empty-statement")
    @Override
    void awaitValue(long i) {
        while (maxReadIndex < i)
            ;
    }

    @SuppressWarnings("empty-statement")
    final void postEnq(long i) {
        while (!compareAndSetMaxReadIndex(i, i + 1))
            ;
    }
    private static final long maxReadIndexOffset;

    static {
        try {
            maxReadIndexOffset = unsafe.objectFieldOffset(SingleConsumerArrayPrimitiveQueue.class.getDeclaredField("maxReadIndex"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    /**
     * CAS maxReadIndex field. Used only by postEnq.
     */
    private boolean compareAndSetMaxReadIndex(long expect, long update) {
        return unsafe.compareAndSwapLong(this, maxReadIndexOffset, expect, update);
    }
}
