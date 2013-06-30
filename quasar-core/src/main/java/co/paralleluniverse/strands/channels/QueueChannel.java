/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.common.util.Objects;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.remote.RemoteProxyFactoryService;
import co.paralleluniverse.strands.Condition;
import co.paralleluniverse.strands.DoneSynchronizer;
import co.paralleluniverse.strands.OwnedSynchronizer;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.queues.BasicQueue;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public abstract class QueueChannel<Message> implements Channel<Message>, SelectableReceive, SelectableSend, java.io.Serializable {
    private static final int MAX_SEND_RETRIES = 10;
    final Condition sync;
    private final Condition sendersSync;
    final BasicQueue<Message> queue;
    private final OverflowPolicy overflowPolicy;
    private volatile boolean sendClosed;
    private boolean receiveClosed;

    protected QueueChannel(BasicQueue<Message> queue, OverflowPolicy overflowPolicy, boolean singleConsumer) {
        this.queue = queue;
        this.sync = new OwnedSynchronizer();
        this.overflowPolicy = overflowPolicy;
        this.sendersSync = overflowPolicy == OverflowPolicy.BLOCK ? new SimpleConditionSynchronizer() : null;
    }

    public int capacity() {
        return queue.capacity();
    }

    protected Condition sync() {
        verifySync();
        return sync;
    }

    protected void signalReceivers() {
        sync.signalAll();
    }

    protected void signalAndTryToExecNow() {
        if (sync instanceof OwnedSynchronizer)
            ((OwnedSynchronizer) sync).signalAndTryToExecNow();
        else
            sync.signalAll();
    }

    void signalSenders() {
        if (overflowPolicy == OverflowPolicy.BLOCK)
            sendersSync.signal();
    }

    @Override
    public Condition sendSelector() {
        return sendersSync != null ? sendersSync : DoneSynchronizer.instance;
    }

    @Override
    public Condition receiveSelector() {
        return sync;
    }

    public void sendNonSuspendable(Message message) throws QueueCapacityExceededException {
        if (isSendClosed())
            return;
        if (!queue.enq(message))
            throw new QueueCapacityExceededException();
        signalReceivers();
    }

    @Override
    public void send(Message message) throws SuspendExecution {
        send0(message, false);
    }

    @Override
    public boolean trySend(Message message) {
        if (isSendClosed())
            return true;
        if (queue.enq(message)) {
            signalReceivers();
            return true;
        } else
            return false;
    }

    protected void sendSync(Message message) throws SuspendExecution {
        send0(message, true);
    }

    public void send0(Message message, boolean sync) throws SuspendExecution {
        if (isSendClosed())
            return;
        if (overflowPolicy == OverflowPolicy.BLOCK)
            sendersSync.register();
        try {
            int i = 0;
            while (!queue.enq(message)) {
                onQueueFull(i++);
            }
        } catch (InterruptedException e) {
            Strand.currentStrand().interrupt();
            return;
        } finally {
            if (overflowPolicy == OverflowPolicy.BLOCK)
                sendersSync.unregister();
        }
        if (sync)
            signalAndTryToExecNow();
        else
            signalReceivers();
    }

    private void onQueueFull(int iter) throws SuspendExecution, InterruptedException {
        switch (overflowPolicy) {
            case DROP:
                return;
            case THROW:
                throw new QueueCapacityExceededException();
            case BLOCK:
                sendersSync.await();
                break;
            case BACKOFF:
                if (iter > MAX_SEND_RETRIES)
                    throw new QueueCapacityExceededException();
                if (iter > 5)
                    Strand.sleep((iter - 5) * 5);
                else if (iter > 4)
                    Strand.yield();
        }
    }

    @Override
    public void close() {
        if (!sendClosed) {
            sendClosed = true;
            signalReceivers();
        }
    }

    /**
     * This method must only be called by the channel's owner (the receiver)
     *
     * @return
     */
    @Override
    public boolean isClosed() {
        return receiveClosed;
    }

    boolean isSendClosed() {
        return sendClosed;
    }

    void setReceiveClosed() {
        this.receiveClosed = true;
    }

    @Override
    public Message tryReceive() {
        if (receiveClosed)
            return null;
        final Message m = queue.poll();
        if (m != null)
            signalSenders();
        return m;
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        if (receiveClosed)
            return null;

        Message m;
        sync.register();
        while ((m = queue.poll()) == null) {
            if (isSendClosed()) {
                setReceiveClosed();
                return null;
            }
            sync.await();
        }
        sync.unregister();

        signalSenders();
        return m;
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (receiveClosed)
            return null;
        if (unit == null)
            return receive();
        if (timeout <= 0)
            return tryReceive();

        Message m;

        final long start = System.nanoTime();
        long left = unit.toNanos(timeout);

        sync.register();
        try {
            while ((m = queue.poll()) == null) {
                if (isSendClosed()) {
                    setReceiveClosed();
                    return null;
                }
                sync.await(left, TimeUnit.NANOSECONDS);

                left = start + unit.toNanos(timeout) - System.nanoTime();
                if (left <= 0)
                    return null;
            }
        } finally {
            sync.unregister();
        }

        if (m != null)
            signalSenders();
        return m;
    }

    public Message receiveFromThread() throws InterruptedException {
        try {
            return receive();
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    public Message receiveFromThread(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            return receive(timeout, unit);
        } catch (SuspendExecution ex) {
            throw new AssertionError(ex);
        }
    }

    private void verifySync() {
        if (sync == null)
            throw new IllegalStateException("Owning strand has not been set");
    }

    public int getQueueLength() {
        return queue.size();
    }

    @Override
    public String toString() {
        return "Channel{" + ", sync: " + sync + ", queue: " + Objects.systemToString(queue) + '}';
    }

    protected Object writeReplace() throws java.io.ObjectStreamException {
        return RemoteProxyFactoryService.create(this, null);
    }
}
