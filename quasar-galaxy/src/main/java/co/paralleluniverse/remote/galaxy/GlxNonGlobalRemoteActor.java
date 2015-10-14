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
import co.paralleluniverse.actors.LifecycleListener;
import co.paralleluniverse.galaxy.cluster.NodeChangeListener;
import co.paralleluniverse.galaxy.quasar.Grid;
import co.paralleluniverse.strands.channels.QueueChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author pron
 */
public class GlxNonGlobalRemoteActor<Message> extends GlxRemoteActor<Message> {
    private static final Grid grid;
    private static final Set<RegistryRecord> listenerRegistry = Collections.newSetFromMap(new ConcurrentHashMap<RegistryRecord, Boolean>());

    static {
        try {
            grid = Grid.getInstance();
            grid.cluster().addNodeChangeListener(new NodeChangeListener() {
                @Override
                public void nodeAdded(short id) {
                }

                @Override
                public void nodeSwitched(short id) {
                }

                @Override
                public void nodeRemoved(short id) {
                    for (Iterator<RegistryRecord> it = listenerRegistry.iterator(); it.hasNext();) {
                        RegistryRecord registryRecord = it.next();
                        if (registryRecord.getOwnerNodeId() == id) {
                            registryRecord.listener.dead(registryRecord.actor.ref(), new Throwable("cluster node removed"));
                            //TODO: remove listeners
                            it.remove();
                        }
                    }
                }
            });
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public GlxNonGlobalRemoteActor(ActorRef<Message> actor) {
        super(actor);
        startReceiver();
    }

    private void startReceiver() {
        final ActorImpl<Message> actor = getActor();
        if (actor == null)
            throw new IllegalStateException("Actor for " + this + " not running locally");

        final RemoteChannelReceiver<Object> receiver = RemoteChannelReceiver.getReceiver((QueueChannel<Object>) getActor().getMailbox());
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

    private boolean isNodeAlive() {
        final short ownerNodeId = getOwnerNodeId();
        return grid.cluster().getMyNodeId() == ownerNodeId || grid.cluster().getNodes().contains(ownerNodeId);
    }

    @Override
    protected void addLifecycleListener(LifecycleListener listener) {
        if (!isNodeAlive()) {
            listener.dead(ref(), null);
            return;
        }
        super.addLifecycleListener(listener);
        listenerRegistry.add(new RegistryRecord(listener, this));
    }

    @Override
    protected void removeLifecycleListener(LifecycleListener listener) {
        super.removeLifecycleListener(listener);
        listenerRegistry.remove(new RegistryRecord(listener, this));
    }

    @Override
    protected void removeObserverListeners(ActorRef actor) {
        super.removeObserverListeners(actor);
        for (Iterator<RegistryRecord> it = listenerRegistry.iterator(); it.hasNext();) {
            RegistryRecord registryRecord = it.next();
            if (registryRecord.actor.equals(this))
                it.remove();
        }
    }

    short getOwnerNodeId() {
        return (short) ((GlxRemoteChannel) getMailbox()).getId().getAddress();
    }

    static final class RegistryRecord {
        final LifecycleListener listener;
        final GlxNonGlobalRemoteActor actor;

        public RegistryRecord(LifecycleListener listener, GlxNonGlobalRemoteActor actor) {
            this.listener = listener;
            this.actor = actor;
        }

        short getOwnerNodeId() {
            return actor.getOwnerNodeId();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + Objects.hashCode(this.listener);
            hash = 79 * hash + Objects.hashCode(this.actor);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (!(obj instanceof RegistryRecord))
                return false;
            final RegistryRecord other = (RegistryRecord) obj;
            if (!Objects.equals(this.listener, other.listener))
                return false;
            if (!Objects.equals(this.actor, other.actor))
                return false;
            return true;
        }
    }
}
