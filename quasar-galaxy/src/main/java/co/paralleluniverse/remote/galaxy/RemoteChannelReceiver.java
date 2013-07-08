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
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.MessageListener;
import co.paralleluniverse.galaxy.quasar.Messenger;
import co.paralleluniverse.io.serialization.Serialization;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.QueueChannel;
import co.paralleluniverse.strands.channels.SendPort;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
    private static final ConcurrentMap<WeakReference<? extends SendPort<?>>, RemoteChannelReceiver<?>> receivers = new ConcurrentHashMapV8<>();
    private static final ReferenceQueue<QueueChannel> refQueue = new ReferenceQueue<>();
    private static final AtomicLong topicGen = new AtomicLong(1000);

    static {
        Thread collector = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    collectDeadReceivers();
                } catch (InterruptedException e) {
                }
            }
        }, "remote-channel-receiver-collector");
        collector.setDaemon(true);
        collector.start();
    }

    public static <Message> RemoteChannelReceiver<Message> getReceiver(SendPort<Message> channel, boolean global) {
        WeakReference<SendPort<Message>> channelRef = new WellBehavedWeakRef(channel, refQueue);
        RemoteChannelReceiver<Message> receiver = (RemoteChannelReceiver<Message>) receivers.get(channelRef);
        if (receiver == null) {
            receiver = createrReceiver(channel, global);
            RemoteChannelReceiver<Message> tmp = (RemoteChannelReceiver<Message>) receivers.putIfAbsent(channelRef, receiver);
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

    public interface MessageFilter<Message> {
        boolean shouldForwardMessage(Message msg);
    }
    //////////////////////////////
    private final SendPort<Message> channel;
    private final Object topic;
    private volatile MessageFilter<Message> filter;

    private RemoteChannelReceiver(SendPort<Message> channel, boolean isGlobal) {
        this.channel = channel;
        this.topic = isGlobal ? UUID.randomUUID().toString() : topicGen.incrementAndGet();
    }

    public void setFilter(MessageFilter<Message> filter) {
        this.filter = filter;
    }

    @Override
    public void messageReceived(short fromNode, byte[] message) {
        Object m1 = Serialization.read(message);
        LOG.debug("Received: "+m1);
        if(m1 instanceof GlxRemoteChannel.CloseMessage) {
            channel.close();
            unsubscribe();
            return;
        }

        final Message m = (Message) m1;
        if (filter == null || filter.shouldForwardMessage(m)) {
            try {
                FiberUtil.runInFiberRuntime(new SuspendableRunnable() {
                    @Override
                    public void run() throws SuspendExecution, InterruptedException {
                        channel.send(m);
                    }
                });
            } catch (InterruptedException e) {
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

    /////////////////////////////
    private static void collectDeadReceivers() throws InterruptedException {
        for (;;) {
            WeakReference<QueueChannel<?>> ref = (WeakReference<QueueChannel<?>>) refQueue.remove();
            // we can't use map.get() b/c the map is organized by WellBehavedWeakRef's hashCode, and here we need identity
            for (Iterator<Map.Entry<WeakReference<? extends SendPort<?>>, RemoteChannelReceiver<?>>> it = receivers.entrySet().iterator(); it.hasNext();) {
                final Map.Entry<WeakReference<? extends SendPort<?>>, RemoteChannelReceiver<?>> entry = it.next();
                if (entry.getKey() == ref) { // using identity
                    final RemoteChannelReceiver<?> receiver = entry.getValue();
                    receiver.unsubscribe();
                    it.remove();
                }
            }
        }
    }

    public static class WellBehavedWeakRef<T> extends WeakReference<T> {
        public WellBehavedWeakRef(T referent) {
            super(referent);
        }

        public WellBehavedWeakRef(T referent, ReferenceQueue<? super T> q) {
            super(referent, q);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(get());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof WellBehavedWeakRef))
                return false;
            return Objects.equals(get(), ((WellBehavedWeakRef) obj).get());
        }
    }
}
