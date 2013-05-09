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
import co.paralleluniverse.strands.queues.SingleConsumerArrayObjectQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayObjectQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class should only be used by actors
 *
 * @author pron
 */
public final class Mailbox<Message> extends Channel<Message> {
    public static <Message> Mailbox<Message> create(Object owner, int mailboxSize) {
        return new Mailbox(owner, mailboxSize > 0 ? new SingleConsumerArrayObjectQueue<Message>(mailboxSize) : new SingleConsumerLinkedArrayObjectQueue<Message>());
    }

    public static <Message> Mailbox<Message> create(int mailboxSize) {
        return new Mailbox(mailboxSize > 0 ? new SingleConsumerArrayObjectQueue<Message>(mailboxSize) : new SingleConsumerLinkedArrayObjectQueue<Message>());
    }

    private Mailbox(Object owner, SingleConsumerQueue<Message, ?> queue) {
        super(owner, queue);
    }

    private Mailbox(SingleConsumerQueue<Message, ?> queue) {
        super(queue);
    }

    public Object succ(Object n) {
        return queue.succ(n);
    }

    public Object del(Object n) {
        return queue.del(n);
    }

    public Message value(Object n) {
        return queue.value(n);
    }

    @Override
    public void maybeSetCurrentStrandAsOwner() {
        super.maybeSetCurrentStrandAsOwner();
    }

    public void lock() {
        sync().lock();
    }

    public void unlock() {
        sync().unlock();
    }

    public void await() throws SuspendExecution, InterruptedException {
            sync().lock();
            sync().await();
        }

    public void await(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        sync().await(timeout, unit);
    }
}
