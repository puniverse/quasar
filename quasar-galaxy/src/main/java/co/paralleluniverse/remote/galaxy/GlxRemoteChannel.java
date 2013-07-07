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

import co.paralleluniverse.common.io.Streamable;
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.Cluster;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.galaxy.quasar.Grid;
import co.paralleluniverse.galaxy.quasar.Messenger;
import co.paralleluniverse.io.serialization.Serialization;
import co.paralleluniverse.remote.RemoteException;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class GlxRemoteChannel<Message> implements SendPort<Message>, Serializable {
    private static final Grid grid;

    static {
        try {
            grid = new Grid(co.paralleluniverse.galaxy.Grid.getInstance());
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    static Messenger getMessenger() {
        return grid.messenger();
    }

    static Cluster getCluster() {
        return grid.cluster();
    }
    private final Object topic; // serializable (String or Long)
    private final long address; // either my node or my ref
    private final boolean global;
    private final short ownerNodeId;

    /**
     * Used on the creating (receiving) side
     *
     * @param channel
     */
    public GlxRemoteChannel(SendPort<Message> channel, Object globalId) {
        final RemoteChannelReceiver<Message> receiver = RemoteChannelReceiver.getReceiver(channel, globalId != null);
        this.topic = receiver.getTopic();
        this.ownerNodeId = getCluster().getMyNodeId();
        if (globalId != null) {
            this.address = (Long) globalId;
            this.global = true;
        } else {
            this.address = ownerNodeId;
            this.global = false;
        }
    }

    public short getOwnerNodeId() {
        return ownerNodeId;
    }

    @Override
    public void send(Message message) throws SuspendExecution {
        try {
            if (global) {
                final long ref = address;
                if (message instanceof Streamable) {
                    if (topic instanceof String)
                        getMessenger().sendToOwnerOf(ref, (String) topic, (Streamable) message);
                    else
                        getMessenger().sendToOwnerOf(ref, (Long) topic, (Streamable) message);
                } else {
                    final byte[] buf = Serialization.write(message);
                    if (topic instanceof String)
                        getMessenger().sendToOwnerOf(ref, (String) topic, buf);
                    else
                        getMessenger().sendToOwnerOf(ref, (Long) topic, buf);
                }
            } else {
                final short node = (short) address;
                if (message instanceof Streamable) {
                    if (topic instanceof String)
                        getMessenger().send(node, (String) topic, (Streamable) message);
                    else
                        getMessenger().send(node, (Long) topic, (Streamable) message);
                } else {
                    final byte[] buf = Serialization.write(message);
                    if (topic instanceof String)
                        getMessenger().send(node, (String) topic, buf);
                    else
                        getMessenger().send(node, (Long) topic, buf);
                }
            }
        } catch (TimeoutException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean trySend(final Message message) {
        try {
            FiberUtil.runInFiberRuntime(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    send(message);
                }
            });
            return true;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            FiberUtil.runInFiberRuntime(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    ((GlxRemoteChannel) GlxRemoteChannel.this).send(new CloseMessage());
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(this.topic);
        hash = 43 * hash + (int) (this.address ^ (this.address >>> 32));
        hash = 43 * hash + (this.global ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof GlxRemoteChannel))
            return false;
        final GlxRemoteChannel<Message> other = (GlxRemoteChannel<Message>) obj;
        if (!Objects.equals(this.topic, other.topic))
            return false;
        if (this.address != other.address)
            return false;
        if (this.global != other.global)
            return false;
        return true;
    }

    static class CloseMessage implements Serializable {
    }
}
