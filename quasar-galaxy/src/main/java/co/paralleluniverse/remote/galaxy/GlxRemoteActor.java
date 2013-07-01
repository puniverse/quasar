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
package co.paralleluniverse.remote.galaxy;

import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.DefaultFiberPool;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.QueueChannel;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author pron
 */
public class RemoteActor<Message> extends co.paralleluniverse.actors.RemoteActor<Message> {
    public RemoteActor(final LocalActor<Message, ?> actor, Object globalId) {
        super(actor);
        final RemoteChannelReceiver<Object> receiver = RemoteChannelReceiver.getReceiver((QueueChannel<Object>) actor.getMailbox(), globalId != null);
        receiver.setFilter(new RemoteChannelReceiver.MessageFilter<Object>() {
            @Override
            public boolean shouldForwardMessage(Object msg) {
                if (msg instanceof RemoteActorAdminMessage) {
                    handleAdminMessage((RemoteActorAdminMessage) msg);
                    return false;
                }
                return true;
            }
        });
    }

    @Override
    protected void internalSend(Object message) throws SuspendExecution {
        ((RemoteChannel) mailbox()).send(message);
    }

    @Override
    protected void internalSendNonSuspendable(final Object message) {
        try {
            new Fiber<Void>(DefaultFiberPool.getInstance()) {
                @Override
                protected Void run() throws SuspendExecution, InterruptedException {
                    internalSend(message);
                    return null;
                }
            }.start().get();
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
        if (!(obj instanceof RemoteActor))
            return false;
        final RemoteActor<Message> other = (RemoteActor<Message>) obj;
        if (!Objects.equals(this.mailbox(), other.mailbox()))
            return false;
        return true;
    }
    
    static Class getActorLifecycleListenerClass() {
        return ActorLifecycleListener.class;
    }
}
