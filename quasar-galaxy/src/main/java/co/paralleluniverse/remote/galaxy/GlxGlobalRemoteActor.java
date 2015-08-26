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

import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.actors.ActorRef;

/**
 *
 * @author pron
 */
public class GlxGlobalRemoteActor<Message> extends GlxRemoteActor<Message> {
    private final long id;

    public GlxGlobalRemoteActor(final ActorRef<Message> actor, Object globalId) {
        super(actor);
        this.id = (Long) globalId;
        startReceiver();
    }

    private void startReceiver() {
        final ActorImpl<Message> actor = getActor();
        if (actor == null)
            throw new IllegalStateException("Actor for " + this + " not running locally");
        GlobalRemoteChannelReceiver.getReceiver(actor.getMailbox(), id);
    }

    @Override
    protected Object readResolve() throws java.io.ObjectStreamException {
            final GlxGlobalRemoteActor remote = (GlxGlobalRemoteActor) super.readResolve();
            // Grid.getInstance().store().setListener(id, remote);
            return remote;
    }
}
