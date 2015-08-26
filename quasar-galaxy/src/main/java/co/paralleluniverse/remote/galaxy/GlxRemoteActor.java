/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.remote.galaxy;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.RemoteActor;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public abstract class GlxRemoteActor<Message> extends RemoteActor<Message> {
    private static final Logger LOG = LoggerFactory.getLogger(GlxRemoteActor.class);
    private static Canonicalizer<GlxGlobalChannelId, GlxRemoteActor> canonicalizer = new Canonicalizer<>();

    public GlxRemoteActor(final ActorRef<Message> actor) {
        super(actor);
    }

    @Override
    protected void internalSend(Object message) throws SuspendExecution {
        ((GlxRemoteChannel) mailbox()).send(message);
    }

    @Override
    protected void internalSendNonSuspendable(final Object message) {
        try {
            new Fiber<Void>(DefaultFiberScheduler.getInstance()) {
                @Override
                protected Void run() throws SuspendExecution, InterruptedException {
                    internalSend(message);
                    return null;
                }
            }.start().joinNoSuspend();
        } catch (ExecutionException e) {
            throw Exceptions.rethrow(e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(mailbox());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof GlxRemoteActor))
            return false;
        final GlxRemoteActor<Message> other = (GlxRemoteActor<Message>) obj;
        if (!Objects.equals(this.mailbox(), other.mailbox()))
            return false;
        return true;
    }

    static Class getActorLifecycleListenerClass() {
        return ActorLifecycleListener.class;
    }

    public GlxGlobalChannelId getId() {
        return ((GlxRemoteChannel) mailbox()).getId();
    }

    protected Object readResolve() throws java.io.ObjectStreamException {
        return canonicalizer.get(getId(), this);
    }

    protected static GlxRemoteActor getImpl(ActorRef<?> actor) {
        return (GlxRemoteActor) RemoteActor.getImpl(actor);
    }
}
