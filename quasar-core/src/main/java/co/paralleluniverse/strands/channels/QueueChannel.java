/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.DelegatingEquals;
import co.paralleluniverse.common.util.Objects;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.remote.RemoteChannelProxyFactoryService;
import co.paralleluniverse.strands.Condition;
import co.paralleluniverse.strands.OwnedSynchronizer;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Synchronization;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.queues.BasicQueue;
import co.paralleluniverse.strands.queues.CircularBuffer;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public abstract class QueueChannel<Message> implements StandardChannel<Message>, Selectable<Message>, Synchronization, java.io.Serializable {
    private static final int MAX_SEND_RETRIES = 10;

    final BasicQueue<Message> queue;
    private final boolean singleProducer;
    private final boolean singleConsumer;
    final Condition sync;
    final Condition sendersSync;
    final OverflowPolicy overflowPolicy;
    private Throwable closeException;
    private volatile boolean sendClosed;
    private boolean receiveClosed;

    protected QueueChannel(BasicQueue<Message> queue, OverflowPolicy overflowPolicy, boolean singleConsumer) {
        this(queue, overflowPolicy, false, singleConsumer);
    }

    protected QueueChannel(BasicQueue<Message> queue, OverflowPolicy overflowPolicy, boolean singleProducer, boolean singleConsumer) {
        this.queue = queue;
        if (!singleConsumer || queue instanceof CircularBuffer)
            this.sync = new SimpleConditionSynchronizer(this);
        else
            this.sync = new OwnedSynchronizer(this);

        this.overflowPolicy = overflowPolicy;
        this.sendersSync = overflowPolicy == OverflowPolicy.BLOCK ? new SimpleConditionSynchronizer(this) : null;
        this.singleProducer = singleProducer;
        this.singleConsumer = singleConsumer;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DelegatingEquals)
            return other.equals(this);
        return super.equals(other);
    }

    @Override
    public int capacity() {
        return queue.capacity();
    }

    @Override
    public boolean isSingleProducer() {
        return singleProducer;
    }

    @Override
    public boolean isSingleConsumer() {
        return singleConsumer;
    }

    public OverflowPolicy getOverflowPolicy() {
        return overflowPolicy;
    }

    protected Condition sync() {
        verifySync();
        return sync;
    }

    protected void signalReceivers() {
        record("signalReceivers", "");
        sync.signalAll();
    }

    protected void signalAndWait() throws SuspendExecution, InterruptedException {
        record("signalAndWait", "");
        if (sync instanceof OwnedSynchronizer)
            ((OwnedSynchronizer) sync).signalAndWait();
        else
            sync.signalAll();
    }

    void signalSenders() {
        if (overflowPolicy == OverflowPolicy.BLOCK) {
            record("signalSenders", "");
            sendersSync.signal();
        }
    }

    @Override
    public Object register(SelectAction<Message> action) {
        if (((SelectActionImpl) action).isData()) {
            if (sendersSync != null)
                sendersSync.register();
        } else
            sync.register();
        return action;
    }

    @Override
    public Object register() {
        // for queues, a simple registration is always a receive
        return sync.register();
    }

    @Override
    public boolean tryNow(Object token) {
        SelectActionImpl<Message> action = (SelectActionImpl<Message>) token;
        if (!action.lease())
            return false;
        boolean res;
        if (action.isData()) {
            res = trySend(action.message());
            if (res)
                action.setItem(null);
        } else {
            Message m = tryReceive();
            action.setItem(m);
            if (m == null)
                res = isClosed();
            else
                res = true;
        }
        if (res)
            action.won();
        else
            action.returnLease();
        return res;
    }

    @Override
    public void unregister(Object token) {
        if (token == null)
            return;
        SelectActionImpl<Message> action = (SelectActionImpl<Message>) token;
        if (action.isData()) {
            if (sendersSync != null)
                sendersSync.unregister(null);
        } else
            sync.unregister(null);
    }

    @Override
    public void send(Message message) throws SuspendExecution, InterruptedException {
        send0(message, false, false, 0);
    }

    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return send0(message, false, true, unit.toNanos(timeout));
    }

    @Override
    public boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return send0(message, false, true, timeout.nanosLeft());
    }

    @Override
    public boolean trySend(Message message) {
        if (message == null)
            throw new IllegalArgumentException("message is null");
        if (isSendClosed())
            return true;
        if (queue.enq(message)) {
            signalReceivers();
            return true;
        } else
            return false;
    }

    protected void sendSync(Message message) throws SuspendExecution {
        try {
            send0(message, true, false, 0);
        } catch (InterruptedException e) {
            Strand.currentStrand().interrupt();
        }
    }

    public boolean send0(Message message, boolean sync, boolean timed, long nanos) throws SuspendExecution, InterruptedException {
        if (message == null)
            throw new IllegalArgumentException("message is null");
        if (isSendClosed())
            return true;
        if (overflowPolicy == OverflowPolicy.BLOCK)
            sendersSync.register();
        try {
            int i = 0;

            final long deadline = timed ? System.nanoTime() + nanos : 0L;

            record("send0", "%s enqueing message %s", this, message);
            while (!queue.enq(message)) {
                if (isSendClosed()) {
                    record("send0", "%s channel is closed for send. Dropping message %s", this, message);
                    return true;
                }
                record("send0", "%s channel queue is full. policy: %s", this, overflowPolicy);
                if (!onQueueFull(i++, timed, nanos))
                    return true;

                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0)
                        throw new TimeoutException();
                }
            }
        } catch (TimeoutException e) {
            return false;
        } finally {
            if (overflowPolicy == OverflowPolicy.BLOCK)
                sendersSync.unregister(null);
        }
        if (sync)
            signalAndWait();
        else
            signalReceivers();
        return true;
    }

    private boolean onQueueFull(int iter, boolean timed, long nanos) throws SuspendExecution, InterruptedException, TimeoutException {
        switch (overflowPolicy) {
            case DROP:
                return false;
            case THROW:
                throw new QueueCapacityExceededException();
            case BLOCK:
                if (timed)
                    sendersSync.await(iter, nanos, TimeUnit.NANOSECONDS);
                else
                    sendersSync.await(iter);
                return true;
            case BACKOFF:
                if (iter > MAX_SEND_RETRIES)
                    throw new QueueCapacityExceededException();
                if (iter > 5)
                    Strand.sleep((iter - 5) * 5);
                else if (iter > 4)
                    Strand.yield();
                return true;
            default:
                throw new AssertionError("Unsupportd policy: " + overflowPolicy);
        }
    }

    @Override
    public void close() {
        if (!sendClosed) {
            sendClosed = true;
            signalReceivers();
            if (sendersSync != null)
                sendersSync.signalAll();
        }
    }

    @Override
    public void close(Throwable t) {
        if (!sendClosed) // possible race here, but it's OK – we just let one of the concurrent exceptions through
            closeException = t;
        close();
    }

    public void sendNonSuspendable(Message message) throws QueueCapacityExceededException {
        if (isSendClosed()) {
            record("sendNonSuspendable", "%s channel is closed for send. Dropping message %s", this, message);
            return;
        }
        record("sendNonSuspendable", "%s enqueing message %s", this, message);
        if (!queue.enq(message))
            throw new QueueCapacityExceededException();
        signalReceivers();
    }

    /**
     * This method must only be called by the channel's owner (the receiver)
     */
    @Override
    public boolean isClosed() {
        if (receiveClosed)
            return true;
        // racy, but that's OK because we don't guarantee anything if we return false
        if (sendClosed && queue.isEmpty()) {
            setReceiveClosed();
            return true;
        }
        return false;
    }

    boolean isSendClosed() {
        return sendClosed;
    }

    void setReceiveClosed() {
        this.receiveClosed = true;
    }

    protected Throwable getCloseException() {
        return closeException;
    }

    private Message closeValue() {
        if (closeException != null)
            throw new ProducerException(closeException);
        return null;
    }

    @Override
    public Message tryReceive() {
        if (receiveClosed)
            return closeValue();
        boolean closed = isSendClosed();
        final Message m = queue.poll();
        if (m != null)
            signalSenders();
        else if (closed) {
            setReceiveClosed();
            return closeValue();
        }
        return m;
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        if (receiveClosed)
            return closeValue();

        Message m;
        boolean closed;
        final Object token = sync.register();
        try {
            for (int i = 0;; i++) {
                closed = isSendClosed(); // must be read BEFORE queue.poll()
                if ((m = queue.poll()) != null)
                    break;

                // i can be > 0 if task state is LEASED
                if (closed) {
                    setReceiveClosed();
                    return closeValue();
                }

                sync.await(i);
            }
        } finally {
            sync.unregister(token);
        }

        assert m != null;
        signalSenders();
        return m;
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (receiveClosed)
            return closeValue();
        if (unit == null)
            return receive();
        if (timeout <= 0)
            return tryReceive();

        long left = unit.toNanos(timeout);
        final long deadline = System.nanoTime() + left;

        Message m;
        boolean closed;
        final Object token = sync.register();
        try {
            for (int i = 0;; i++) {
                closed = isSendClosed(); // must be read BEFORE queue.poll()
                if ((m = queue.poll()) != null)
                    break;
                if (closed) {
                    setReceiveClosed();
                    return closeValue();
                }

                sync.await(i, left, TimeUnit.NANOSECONDS);

                left = deadline - System.nanoTime();
                if (left <= 0)
                    return null;
            }
        } finally {
            sync.unregister(token);
        }

        if (m != null)
            signalSenders();
        return m;
    }

    @Override
    public Message receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
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
        return "Channel{" + "sync: " + sync + ", queue: " + Objects.systemToString(queue) + ", capacity: " + capacity() + '}';
    }

    protected Object writeReplace() throws java.io.ObjectStreamException {
        return RemoteChannelProxyFactoryService.create(this, null);
    }
    ////////////////////////////
    public static final FlightRecorder RECORDER = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;

    boolean isRecording() {
        return RECORDER != null;
    }

    static void record(String method, String format) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("QueueChannel", method, format, null));
    }

    static void record(String method, String format, Object arg1) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("QueueChannel", method, format, new Object[]{arg1}));
    }

    static void record(String method, String format, Object arg1, Object arg2) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("QueueChannel", method, format, new Object[]{arg1, arg2}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("QueueChannel", method, format, new Object[]{arg1, arg2, arg3}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("QueueChannel", method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("QueueChannel", method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }
}
