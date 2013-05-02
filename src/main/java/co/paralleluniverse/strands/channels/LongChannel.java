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
import co.paralleluniverse.strands.queues.SingleConsumerArrayLongQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayLongQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLongQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class LongChannel extends Channel<Long> {
    public static LongChannel create(Object owner, int mailboxSize) {
        return new LongChannel(owner, mailboxSize > 0 ? new SingleConsumerArrayLongQueue(mailboxSize) : new SingleConsumerLinkedArrayLongQueue());
    }

    public static LongChannel create(int mailboxSize) {
        return new LongChannel(mailboxSize > 0 ? new SingleConsumerArrayLongQueue(mailboxSize) : new SingleConsumerLinkedArrayLongQueue());
    }

    private LongChannel(Object owner, SingleConsumerQueue<Long, ?> queue) {
        super(owner, queue);
    }

    private LongChannel(SingleConsumerQueue<Long, ?> queue) {
        super(queue);
    }

    public long receiveLong() throws SuspendExecution, InterruptedException {
        final Object n = receiveNode();
        final long m = ((SingleConsumerLongQueue<Object>) queue).longValue(n);
        queue.deq(n);
        return m;
    }

    public long receiveLong(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        final Object n = receiveNode(timeout, unit);
        final long m = ((SingleConsumerLongQueue<Object>) queue).longValue(n);
        queue.deq(n);
        return m;
    }

    public void send(long message) {
        queue.enq(message);
        signal();
    }

    public void sendSync(long message) {
        queue.enq(message);
        signalAndTryToExecNow();
    }
}
