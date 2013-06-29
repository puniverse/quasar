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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.remote.RemoteProxyFactoryService;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.QueueChannel;
import co.paralleluniverse.strands.queues.SingleConsumerArrayObjectQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayObjectQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class should only be used by actors
 *
 * @author pron
 */
public final class Mailbox<Message> extends QueueChannel<Message> {
    private transient LocalActor<?, ?> actor;

    Mailbox(MailboxConfig config) {
        this(null, config);
    }

    Mailbox(Strand owner, MailboxConfig config) {
        super(owner,
                mailboxSize(config) > 0
                ? new SingleConsumerArrayObjectQueue<Message>(config.getMailboxSize())
                : new SingleConsumerLinkedArrayObjectQueue<Message>(),
                overflowPolicy(config));
    }

    private static int mailboxSize(MailboxConfig config) {
        return config != null ? config.getMailboxSize() : -1;
    }
    private static OverflowPolicy overflowPolicy(MailboxConfig config) {
        return config != null ? config.getPolicy() : OverflowPolicy.THROW;
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
    protected void sendSync(Message message) throws SuspendExecution {
        super.sendSync(message);
    }

    @Override
    public void maybeSetCurrentStrandAsOwner() {
        super.maybeSetCurrentStrandAsOwner();
    }

    public void lock() {
        sync().register();
    }

    public void unlock() {
        sync().unregister();
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
