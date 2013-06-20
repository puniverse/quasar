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
package co.paralleluniverse.strands.channels.galaxy;

import co.paralleluniverse.actors.galaxy.Serializer;
import co.paralleluniverse.common.io.Streamable;
import co.paralleluniverse.galaxy.Cluster;
import co.paralleluniverse.galaxy.Grid;
import co.paralleluniverse.galaxy.Messenger;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.remote.RemoteException;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.SendChannel;
import java.io.Serializable;

/**
 *
 * @author pron
 */
public class RemoteChannel<Message> implements SendChannel<Message>, Serializable {
    private static volatile Grid grid;

    static void setGrid(Grid grid) {
        RemoteChannel.grid = grid;
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
    public void send(Message message) {
        try {
            if (global) {
                final long ref = address;
                if (message instanceof Streamable) {
                    if (topic instanceof String)
                        getMessenger().sendToOwnerOf(ref, (String) topic, (Streamable) message);
                    else
                        getMessenger().sendToOwnerOf(ref, (Long) topic, (Streamable) message);
                } else {
                    final byte[] buf = Serializer.serialize(message);
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
                    final byte[] buf = Serializer.serialize(message);
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
}
