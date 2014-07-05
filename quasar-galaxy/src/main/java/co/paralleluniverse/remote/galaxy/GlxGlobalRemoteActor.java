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
package co.paralleluniverse.remote.galaxy;

import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.Cache;
import co.paralleluniverse.galaxy.CacheListener;
import co.paralleluniverse.galaxy.quasar.Grid;
import java.nio.ByteBuffer;

/**
 *
 * @author pron
 */
public class GlxGlobalRemoteActor<Message> extends GlxRemoteActor<Message> implements CacheListener {
    private final long ref;

    public GlxGlobalRemoteActor(final ActorRef<Message> actor, Object globalId) {
        super(actor);
        this.ref = (Long) globalId;
        startReceiver();
    }

    private void startReceiver() {
        final ActorImpl<Message> actor = getActor();
        if (actor == null)
            throw new IllegalStateException("Actor for " + this + " not running locally");
        GlobalRemoteChannelReceiver.getReceiver(actor.getMailbox(), ref);
    }

    @Override
    protected Object readResolve() throws java.io.ObjectStreamException, SuspendExecution {
        try {
            GlxGlobalRemoteActor remote = (GlxGlobalRemoteActor) super.readResolve();
            CacheListener l1 = new Grid(co.paralleluniverse.galaxy.Grid.getInstance()).store().setListenerIfAbsent(ref, remote);
            assert l1 == remote;
            return remote;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void killed(Cache cache, long id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void invalidated(Cache cache, long id) {
    }

    @Override
    public void received(Cache cache, long id, long version, ByteBuffer data) {
    }

    @Override
    public void evicted(Cache cache, long id) {
    }

    @Override
    public void messageReceived(byte[] message) {
        throw new RuntimeException("Received unexpected message (" + message.length + " bytes)");
    }
}
