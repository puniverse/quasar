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
import co.paralleluniverse.strands.queues.SingleConsumerIntQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class QueueIntChannel extends QueuePrimitiveChannel<Integer> implements IntChannel {
    public QueueIntChannel(SingleConsumerQueue<Integer, ?> queue, OverflowPolicy policy) {
        super(queue, policy);
    }

    @Override
    public int receiveInt() throws SuspendExecution, InterruptedException {
        if (isClosed())
            throw new EOFException();
        final Object n = receiveNode();
        final int m = queue1().intValue(n);
        queue().deq(n);
        signalSenders();
        return m;
    }

    @Override
    public int receiveInt(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException {
        if (isClosed())
            throw new EOFException();
        final Object n = receiveNode(timeout, unit);
        if (n == null)
            throw new TimeoutException();
        final int m = queue1().intValue(n);
        queue().deq(n);
        signalSenders();
        return m;
    }

    @Override
    public void send(int message) {
        if (isSendClosed())
            return;
        queue().enq(message);
        signalReceivers();
    }

    @Override
    public boolean trySend(int message) {
        if (isSendClosed())
            return true;
        if (queue().enq(message)) {
            signalReceivers();
            return true;
        } else
            return false;
    }

    public void sendSync(int message) {
        if (isSendClosed())
            return;
        queue().enq(message);
        signalAndTryToExecNow();
    }

    private SingleConsumerIntQueue<Object> queue1() {
        return (SingleConsumerIntQueue<Object>) queue;
    }
}
