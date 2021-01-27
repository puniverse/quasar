/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.queues.BasicSingleConsumerFloatQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class QueueFloatChannel extends QueuePrimitiveChannel<Float> implements FloatChannel {
    public QueueFloatChannel(BasicSingleConsumerFloatQueue queue, OverflowPolicy policy) {
        super(queue, policy);
    }

    @Override
    public float receiveFloat() throws SuspendExecution, InterruptedException, EOFException {
        checkClosed();
        awaitItem();
        final float m = queue().pollFloat();
        signalSenders();
        return m;
    }

    @Override
    public float receiveFloat(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
        checkClosed();
        if (!awaitItem(timeout, unit))
            throw new TimeoutException();
        final float m = queue().pollFloat();
        signalSenders();
        return m;
    }

    @Override
    public float receiveFloat(Timeout timeout) throws SuspendExecution, InterruptedException, TimeoutException, EOFException {
        return receiveFloat(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
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

    @Override
    public void send(float message) throws SuspendExecution, InterruptedException {
        if (isSendClosed())
            return;
        if (!queue().enq(message))
            super.send(message);
        else
            signalReceivers();
    }

    @Override
    public boolean send(float message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (isSendClosed())
            return true;
        if (!queue().enq(message))
            return super.send(message, timeout, unit);
        signalReceivers();
        return true;
    }

    @Override
    public boolean send(float message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    @Override
    protected BasicSingleConsumerFloatQueue queue() {
        return (BasicSingleConsumerFloatQueue) queue;
    }
}
