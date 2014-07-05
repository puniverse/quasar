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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.Cache;
import co.paralleluniverse.galaxy.CacheListener;
import co.paralleluniverse.galaxy.Store;
import co.paralleluniverse.io.serialization.Serialization;
import co.paralleluniverse.strands.channels.SendPort;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to messages received from remote ends of a channel, and forwards them to the right channel.
 *
 */
public class GlobalRemoteChannelReceiver<Message> implements CacheListener {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalRemoteChannelReceiver.class);
    private static final Store store;

    static {
        try {
            store = co.paralleluniverse.galaxy.Grid.getInstance().store();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static <Message> GlobalRemoteChannelReceiver<Message> getReceiver(SendPort<Message> channel, long ref) {
        return (GlobalRemoteChannelReceiver)store.setListenerIfAbsent(ref, new GlobalRemoteChannelReceiver(channel, ref));
    }

    public static void stopReceiver(long ref) {
        store.setListener(ref, null);
    }

    public interface MessageFilter<Message> {
        boolean shouldForwardMessage(Message msg);
    }
    
    //////////////////////////////
    private final SendPort<Message> channel;
    private final long ref;
    private volatile MessageFilter<Message> filter;

    private GlobalRemoteChannelReceiver(SendPort<Message> channel, long ref) {
        this.channel = channel;
        this.ref = ref;
    }

    public void setFilter(MessageFilter<Message> filter) {
        this.filter = filter;
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
    public void killed(Cache cache, long id) {
    }

    
    @Override
    public void messageReceived(byte[] message) {
        Object m1 = Serialization.getInstance().read(message);
        LOG.debug("Received: " + m1);
        if (m1 instanceof GlxRemoteChannel.CloseMessage) {
            Throwable t = ((GlxRemoteChannel.CloseMessage) m1).getException();
            if (t != null)
                channel.close(t);
            else
                channel.close();
            unsubscribe();
            return;
        } else if (m1 instanceof GlxRemoteChannel.RefMessage) {
            return;
        }

        final Message m = (Message) m1;
        if (filter == null || filter.shouldForwardMessage(m)) {
            try {
                channel.send(m); // TODO: this may potentially block the whole messenger thread!!!
            } catch (SuspendExecution e) {
                throw new AssertionError(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void unsubscribe() {
        store.setListener(ref, null);
    }
}
