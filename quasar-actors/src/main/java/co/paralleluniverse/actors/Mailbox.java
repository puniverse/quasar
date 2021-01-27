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
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.remote.RemoteChannelProxyFactoryService;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.channels.SingleConsumerQueueChannel;
import co.paralleluniverse.strands.queues.SingleConsumerArrayObjectQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayObjectQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A channel that is used as an actor's mailbox.
 * This class should only be used by actors
 *
 * @author pron
 */
public final class Mailbox<Message> extends SingleConsumerQueueChannel<Message> {
    private transient Actor<?, ?> actor;
    private Object registrationToken;
    private final MailboxConfig config;

    Mailbox(MailboxConfig config) {
        super(mailboxSize(config) > 0
                ? new SingleConsumerArrayObjectQueue<Message>(config.getMailboxSize())
                : new SingleConsumerLinkedArrayObjectQueue<Message>(),
                overflowPolicy(config));
        this.config = config;
    }

    private static int mailboxSize(MailboxConfig config) {
        return config != null ? config.getMailboxSize() : -1;
    }

    private static OverflowPolicy overflowPolicy(MailboxConfig config) {
        return config != null ? config.getPolicy() : OverflowPolicy.THROW;
    }

    void setActor(Actor<?, ?> actor) {
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

    protected SingleConsumerQueue<Message> queue() {
        return super.queue();
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
        registrationToken = sync().register();
    }

    public void unlock() {
        sync().unregister(registrationToken);
    }

    public void await(int iter) throws SuspendExecution, InterruptedException {
        sync().await(iter);
    }

    public void await(int iter, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        sync().await(iter, timeout, unit);
    }

    @Override
    protected Object writeReplace() throws java.io.ObjectStreamException {
        if (actor.isMigrating())
            return new SerializedMailbox(config);
        if (!actor.isStarted())
            throw new IllegalStateException("Owning actor " + actor + " not started");

        return RemoteChannelProxyFactoryService.create(this, actor.getGlobalId());
    }

    List<Message> getSnapshot() {
        return queue().snapshot();
    }

    private static class SerializedMailbox implements java.io.Serializable {
        private final MailboxConfig config;

        public SerializedMailbox(MailboxConfig config) {
            this.config = config;
        }

        protected Object readResolve() throws java.io.ObjectStreamException {
            return new Mailbox(config);
        }
    }
}
