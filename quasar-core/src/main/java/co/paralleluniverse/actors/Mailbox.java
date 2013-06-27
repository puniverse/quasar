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
package co.paralleluniverse.actors;

import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.remote.RemoteProxyFactoryService;
import co.paralleluniverse.strands.channels.Channel;
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
    public static <Message> Mailbox<Message> create(int mailboxSize) {
        return new Mailbox(mailboxSize > 0 ? new SingleConsumerArrayObjectQueue<Message>(mailboxSize) : new SingleConsumerLinkedArrayObjectQueue<Message>());
    }
    private transient LocalActor<?, ?> actor;

    private Mailbox(SingleConsumerQueue<Message, ?> queue) {
        super(queue);
    }

    private Mailbox(Object owner, SingleConsumerQueue<Message, ?> queue) {
        super(owner, queue);
    }

    void setActor(LocalActor<?, ?> actor) {
        this.actor = actor;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        return false;
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
        sync().await();
    }

    public void await(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        sync().await(timeout, unit);
    }

    @Override
    protected Object writeReplace() throws java.io.ObjectStreamException {
        return RemoteProxyFactoryService.create(this, actor.getGlobalId());
    }
}
