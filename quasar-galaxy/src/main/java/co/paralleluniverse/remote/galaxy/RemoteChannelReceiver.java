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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.MessageListener;
import co.paralleluniverse.galaxy.cluster.NodeChangeListener;
import co.paralleluniverse.galaxy.quasar.Grid;
import co.paralleluniverse.galaxy.quasar.Messenger;
import co.paralleluniverse.io.serialization.Serialization;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.QueueChannel;
import co.paralleluniverse.strands.channels.SendPort;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import jsr166e.ConcurrentHashMapV8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to messages received from remote ends of a channel, and forwards them to the right channel.
 *
 */
public class RemoteChannelReceiver<Message> implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteChannelReceiver.class);
    private static final ConcurrentMap<SendPort<?>, RemoteChannelReceiver<?>> receivers = new ConcurrentHashMapV8<>();
    private static final ReferenceQueue<QueueChannel> refQueue = new ReferenceQueue<>();
    private static final AtomicLong topicGen = new AtomicLong(1000);

    public static <Message> RemoteChannelReceiver<Message> getReceiver(SendPort<Message> channel, boolean global) {
        RemoteChannelReceiver<Message> receiver = (RemoteChannelReceiver<Message>) receivers.get(channel);
        if (receiver == null) {
            receiver = createrReceiver(channel, global);
            RemoteChannelReceiver<Message> tmp = (RemoteChannelReceiver<Message>) receivers.putIfAbsent(channel, receiver);
            if (tmp == null) {
                receiver.subscribe();
            } else
                receiver = tmp;
        }
        return receiver;
    }

    private static <Message> RemoteChannelReceiver<Message> createrReceiver(SendPort<Message> channel, boolean global) {
        return new RemoteChannelReceiver<Message>(channel, global);
    }

    void shutdown() {
        LOG.debug("shutdown of receiver due to zero references" + this);
        unsubscribe();
        receivers.remove(this.channel);
    }

    public interface MessageFilter<Message> {
        boolean shouldForwardMessage(Message msg);
    }
    //////////////////////////////
    private final SendPort<Message> channel;
    private final Object topic;
    private volatile MessageFilter<Message> filter;
    private final Map<Short, Integer> references = new ConcurrentHashMap<>();

    private RemoteChannelReceiver(SendPort<Message> channel, boolean isGlobal) {
        this.channel = channel;
        this.topic = isGlobal ? UUID.randomUUID().toString() : topicGen.incrementAndGet();
        try {
            new Grid(co.paralleluniverse.galaxy.Grid.getInstance()).cluster().addNodeChangeListener(new NodeChangeListener() {
                @Override
                public void nodeAdded(short id) {
                }

                @Override
                public void nodeSwitched(short id) {
                }

                @Override
                public void nodeRemoved(short id) {
                    LOG.debug("decrease RefCount for {} from node {}", this, id);
                    references.remove(id);
                    if (references.isEmpty())
                        shutdown();
                }
            });
        } catch (InterruptedException ex) {
            LOG.error(ex.toString());
        }

    }

    public void setFilter(MessageFilter<Message> filter) {
        this.filter = filter;
    }

    @Override
    public void messageReceived(short fromNode, byte[] message) {
        Object m1 = Serialization.getInstance().read(message);
        LOG.debug("Received: " + m1);
        if (m1 instanceof GlxRemoteChannel.CloseMessage) {
            channel.close();
            unsubscribe();
            return;
        } else if (m1 instanceof GlxRemoteChannel.RefMessage) {
            handleRefMessage((GlxRemoteChannel.RefMessage) m1);
            return;
        }

        final Message m = (Message) m1;
        if (filter == null || filter.shouldForwardMessage(m)) {
            try {
                channel.send(m); // TODO: this may potentially block the whole messenger thread!!!
            } catch (SuspendExecution e) {
                throw new AssertionError(e);
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void subscribe() {
        final Messenger messenger = GlxRemoteChannel.getMessenger();
        if (topic instanceof String)
            messenger.addMessageListener((String) topic, this);
        else
            messenger.addMessageListener((Long) topic, this);
    }

    private void unsubscribe() {
        final Messenger messenger = GlxRemoteChannel.getMessenger();
        if (topic instanceof String)
            messenger.removeMessageListener((String) topic, this);
        else
            messenger.removeMessageListener((Long) topic, this);
    }

    public Object getTopic() {
        return topic;
    }

    void handleRefMessage(GlxRemoteChannel.RefMessage msg) throws RuntimeException {
        LOG.debug("handling: " + msg);
        if (msg.isAdd()) {
            Integer refCount = references.get(msg.getNodeId());
            if (refCount == null) {
                references.put(msg.getNodeId(), 1);
            } else
                references.put(msg.getNodeId(), refCount + 1);
        } else {
            Integer refCount = references.get(msg.getNodeId());
            if (refCount == null) {
                throw new RuntimeException("decrease reference counter message received for unknown cluster node");
            } else {
                if (--refCount > 0)
                    references.put(msg.getNodeId(), refCount);
                else {
                    references.remove(msg.getNodeId());
                    if (references.isEmpty()) {
                        shutdown();
                    }
                }
            }
        }
    }
}
