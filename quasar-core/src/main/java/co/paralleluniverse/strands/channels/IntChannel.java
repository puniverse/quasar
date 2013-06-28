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
import co.paralleluniverse.strands.queues.SingleConsumerArrayIntQueue;
import co.paralleluniverse.strands.queues.SingleConsumerIntQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayIntQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class IntChannel extends PrimitiveChannel<Integer> {
    public static IntChannel create(Object owner, int mailboxSize, OverflowPolicy policy) {
        return new IntChannel(owner,
                mailboxSize > 0
                ? new SingleConsumerArrayIntQueue(mailboxSize)
                : new SingleConsumerLinkedArrayIntQueue(),
                policy);
    }

    public static IntChannel create(Object owner, int mailboxSize) {
        return create(owner, mailboxSize, OverflowPolicy.THROW);
    }

    public static IntChannel create(int mailboxSize, OverflowPolicy policy) {
        return create(null, mailboxSize, policy);
    }

    public static IntChannel create(int mailboxSize) {
        return create(null, mailboxSize, OverflowPolicy.THROW);
    }

    private IntChannel(Object owner, SingleConsumerQueue<Integer, ?> queue, OverflowPolicy policy) {
        super(owner, queue, policy);
    }

    public int receiveInt() throws SuspendExecution, InterruptedException {
        if (isClosed())
            throw new EOFException();
        final Object n = receiveNode();
        final int m = queue().intValue(n);
        queue.deq(n);
        signalSenders();
        return m;
    }

    public int receiveInt(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException {
        if (isClosed())
            throw new EOFException();
        final Object n = receiveNode(timeout, unit);
        if (n == null)
            throw new TimeoutException();
        final int m = queue().intValue(n);
        queue.deq(n);
        signalSenders();
        return m;
    }

    public void send(int message) {
        if (isSendClosed())
            return;
        queue().enq(message);
        signal();
    }

    public void sendSync(int message) {
        if (isSendClosed())
            return;
        queue().enq(message);
        signalAndTryToExecNow();
    }

    private SingleConsumerIntQueue<Object> queue() {
        return (SingleConsumerIntQueue<Object>) queue;
    }
}
