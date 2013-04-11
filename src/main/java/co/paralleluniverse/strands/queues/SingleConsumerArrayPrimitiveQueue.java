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
    volatile int maxReadIndex;

    @Override
    void clearValue(int index) {
    }

    @SuppressWarnings("empty-statement")
    @Override
    void awaitValue(int i) {
        final boolean greaterThanHead = i >= head;
        while (!(maxReadIndex > i || (greaterThanHead & maxReadIndex < head)));
    }

    @SuppressWarnings("empty-statement")
    final void postEnq(int index) {
        while (!compareAndSetMaxReadIndex(index, next(index)))
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
    private boolean compareAndSetMaxReadIndex(int expect, int update) {
        return unsafe.compareAndSwapInt(this, maxReadIndexOffset, expect, update);
    }
}
