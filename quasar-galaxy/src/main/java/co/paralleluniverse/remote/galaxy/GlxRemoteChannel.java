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
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.Serializable;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class GlxRemoteChannel<Message> implements SendPort<Message>, Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(GlxRemoteChannel.class);
    private static final Grid grid;
    private static final ExecutorService sendThreadPool = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build()); //Executors.newCachedThreadPool();
    private static Canonicalizer<GlxGlobalChannelId, GlxRemoteChannel> canonicalizer = new Canonicalizer<>();

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

    private final GlxGlobalChannelId id;
    private final short ownerNodeId;

    /**
     * Used on the creating (receiving) side
     *
     * @param channel
     */
    public GlxRemoteChannel(SendPort<Message> channel, Object globalId) {
        final RemoteChannelReceiver<Message> receiver = RemoteChannelReceiver.getReceiver(channel, globalId != null);
        final Object topic = receiver.getTopic();
        this.ownerNodeId = getCluster().getMyNodeId();
        if (globalId != null)
            this.id = new GlxGlobalChannelId(true, (Long) globalId, topic);
        else
            this.id = new GlxGlobalChannelId(false, ownerNodeId, topic);
    }

    public GlxGlobalChannelId getId() {
        return id;
    }

    public short getOwnerNodeId() {
        return ownerNodeId;
    }

    @Override
    public void send(Message message) throws SuspendExecution {
        submitSend(message, getId());
    }

    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
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
    public void close(final Throwable t) {
        try {
            FiberUtil.runInFiberRuntime(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    ((GlxRemoteChannel) GlxRemoteChannel.this).send(new CloseMessage(t));
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof GlxRemoteChannel))
            return false;
        final GlxRemoteChannel<Message> other = (GlxRemoteChannel<Message>) obj;
        if (!Objects.equals(this.id, other.id))
            return false;
        return true;
    }

    protected Object readResolve() throws java.io.ObjectStreamException, SuspendExecution {
        GlxRemoteChannel<Message> self = canonicalizer.get(getId(), this);
        new RCPhantomReference(self).register();
        return self;
    }

    private static void registerRemoteRef(final short myNodeId, GlxGlobalChannelId id) throws SuspendExecution {
        submitSend(new RefMessage(true, myNodeId), id);
    }

    private static void unregisterRemoteRef(final short myNodeId, GlxGlobalChannelId id) throws SuspendExecution {
        submitSend(new RefMessage(false, myNodeId), id);
    }

    private static void submitSend(final Object message, final GlxGlobalChannelId id) {
        LOG.debug("sending: " + message);
        sendThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    staticSend(message, id);
                    LOG.debug("sent {}", message);
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }
        });
    }

    private static void staticSend(Object message, GlxGlobalChannelId id) throws SuspendExecution {
        try {
            if (id.global) {
                final long ref = id.address;
                if (message instanceof Streamable) {
                    if (id.topic instanceof String)
                        getMessenger().sendToOwnerOf(ref, (String) id.topic, (Streamable) message);
                    else
                        getMessenger().sendToOwnerOf(ref, (Long) id.topic, (Streamable) message);
                } else {
                    final byte[] buf = Serialization.getInstance().write(message);
                    if (id.topic instanceof String)
                        getMessenger().sendToOwnerOf(ref, (String) id.topic, buf);
                    else
                        getMessenger().sendToOwnerOf(ref, (Long) id.topic, buf);
                }
            } else {
                final short node = (short) id.address;
                if (message instanceof Streamable) {
                    if (id.topic instanceof String)
                        getMessenger().send(node, (String) id.topic, (Streamable) message);
                    else
                        getMessenger().send(node, (Long) id.topic, (Streamable) message);
                } else {
                    final byte[] buf = Serialization.getInstance().write(message);
                    if (id.topic instanceof String)
                        getMessenger().send(node, (String) id.topic, buf);
                    else
                        getMessenger().send(node, (Long) id.topic, buf);
                }
            }
        } catch (TimeoutException e) {
            throw new RemoteException(e);
        }
    }

    private short getNodeId() {
        return getCluster().getMyNodeId();
    }

    static class CloseMessage implements Serializable {
        private final Throwable t;

        public CloseMessage(Throwable t) {
            this.t = t;
        }

        public CloseMessage() {
            this(null);
        }

        public Throwable getException() {
            return t;
        }
    }

    static class RefMessage implements Serializable {
        final boolean add;
        final short nodeId;

        public boolean isAdd() {
            return add;
        }

        public short getNodeId() {
            return nodeId;
        }

        public RefMessage(boolean add, short nodeId) {
            this.add = add;
            this.nodeId = nodeId;
        }

        @Override
        public String toString() {
            return "RefMessage{" + "add=" + add + ", nodeId=" + nodeId + '}';
        }
    }

    static class RCPhantomReference extends PhantomReference<GlxRemoteChannel> {
        private final static Set<RCPhantomReference> rcs = Collections.newSetFromMap(new ConcurrentHashMap<RCPhantomReference, Boolean>());
        private final static ReferenceQueue<GlxRemoteChannel> q = new ReferenceQueue<>();
        final short myNodeId;
        final public GlxGlobalChannelId id;

        public RCPhantomReference(GlxRemoteChannel referent) {
            super(referent, q);
            this.id = referent.id;
            this.myNodeId = referent.getNodeId();
        }

        public void unregister() throws SuspendExecution {
            GlxRemoteChannel.unregisterRemoteRef(myNodeId, id);
            rcs.remove(this);
        }

        public void register() throws SuspendExecution {
            rcs.add(this);
            GlxRemoteChannel.registerRemoteRef(myNodeId, id);
        }

        static {
            Thread collector = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!Thread.interrupted()) {
                            try {
                                final RCPhantomReference ref = (RCPhantomReference) q.remove();
                                FiberUtil.runInFiber(new SuspendableRunnable() {
                                    @Override
                                    public void run() throws SuspendExecution, InterruptedException {
                                        ref.unregister();
                                    }
                                });
                            } catch (ExecutionException e) {
                                LOG.error(e.toString());
                            }
                        }
                    } catch (InterruptedException e) {
                        LOG.info(this.toString() + " has been interrupted");
                    }
                }
            }, "remote-references-collector");
            collector.setDaemon(true);
            collector.start();
        }
    }
}
