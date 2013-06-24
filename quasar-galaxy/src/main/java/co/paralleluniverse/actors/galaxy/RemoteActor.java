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
package co.paralleluniverse.actors.galaxy;

import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.galaxy.RemoteChannel;
import co.paralleluniverse.strands.channels.galaxy.RemoteChannelReceiver;
import java.util.Objects;

/**
 *
 * @author pron
 */
public class RemoteActor<Message> extends co.paralleluniverse.actors.RemoteActor<Message> {
    public RemoteActor(final LocalActor<Message, ?> actor, Object globalId) {
        super(actor, globalId);
        final RemoteChannelReceiver<Object> receiver = RemoteChannelReceiver.getReceiver((Channel<Object>) actor.getMailbox(), globalId != null);
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
    protected boolean isBackpressure() {
        return false;
    }

    @Override
    protected void internalSend(Object message) {
        ((RemoteChannel) mailbox()).send(message);
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
}
