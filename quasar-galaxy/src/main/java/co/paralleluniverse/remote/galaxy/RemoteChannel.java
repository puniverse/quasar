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
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.DefaultFiberPool;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.Cluster;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.galaxy.quasar.Grid;
import co.paralleluniverse.galaxy.quasar.Messenger;
import co.paralleluniverse.io.serialization.Serialization;
import co.paralleluniverse.remote.RemoteException;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author pron
 */
public class RemoteChannel<Message> implements SendPort<Message>, Serializable {
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

    /**
     * Used on the creating (receiving) side
     *
     * @param channel
     */
    public RemoteChannel(Channel channel, Object globalId) {
        final RemoteChannelReceiver<Message> receiver = RemoteChannelReceiver.getReceiver(channel, globalId != null);
        this.topic = receiver.getTopic();
        if (globalId != null) {
            this.address = (Long) globalId;
            this.global = true;
        } else {
            this.address = getCluster().getMyNodeId();
            this.global = false;
        }
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
    public void close() {
        try {
            new Fiber<Void>(DefaultFiberPool.getInstance()) {
                @Override
                protected Void run() throws SuspendExecution, InterruptedException {
                    ((RemoteChannel) RemoteChannel.this).send(new CloseMessage());
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
        hash = 43 * hash + Objects.hashCode(this.topic);
        hash = 43 * hash + (int) (this.address ^ (this.address >>> 32));
        hash = 43 * hash + (this.global ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof RemoteChannel))
            return false;
        final RemoteChannel<Message> other = (RemoteChannel<Message>) obj;
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
