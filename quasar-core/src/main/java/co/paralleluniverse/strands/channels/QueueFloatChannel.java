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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.queues.SingleConsumerFloatQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class QueueFloatChannel extends QueuePrimitiveChannel<Float> implements FloatChannel {
    public QueueFloatChannel(SingleConsumerQueue<Float, ?> queue, OverflowPolicy policy) {
        super(queue, policy);
    }

    @Override
    public float receiveFloat() throws SuspendExecution, InterruptedException {
        if (isClosed())
            throw new EOFException();
        final Object n = receiveNode();
        final float m = queue1().floatValue(n);
        queue().deq(n);
        signalSenders();
        return m;
    }

    @Override
    public float receiveFloat(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException {
        if (isClosed())
            throw new EOFException();
        final Object n = receiveNode(timeout, unit);
        if (n == null)
            throw new TimeoutException();
        final float m = queue1().floatValue(n);
        queue().deq(n);
        signalSenders();
        return m;
    }

    @Override
    public void send(float message) {
        if (isSendClosed())
            return;
        queue().enq(message);
        signalReceivers();
    }

    @Override
    public boolean trySend(float message) {
        if (isSendClosed())
            return true;
        if (queue().enq(message)) {
            signalReceivers();
            return true;
        } else
            return false;
    }

    public void sendSync(float message) {
        if (isSendClosed())
            return;
        queue().enq(message);
        signalAndTryToExecNow();
    }

    private SingleConsumerFloatQueue<Object> queue1() {
        return (SingleConsumerFloatQueue<Object>) queue;
    }
}
