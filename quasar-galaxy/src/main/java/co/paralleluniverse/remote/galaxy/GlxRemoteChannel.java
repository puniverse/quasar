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
    private static final ExecutorService sendThreadPool = Executors.newSingleThreadExecutor(); //Executors.newCachedThreadPool();

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
        submitSend(message, global, address, topic);
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

    protected Object readResolve() throws java.io.ObjectStreamException, SuspendExecution {
        new RCPhantomReference(this).register();
        return this;
    }

    private static void registerRemoteRef(final short myNodeId, final boolean global, final long address, final Object topic) throws SuspendExecution {
        submitSend(new RefMessage(true, myNodeId), global, address, topic);
    }

    private static void unregisterRemoteRef(final short myNodeId, final boolean global, final long address, final Object topic) throws SuspendExecution {
        submitSend(new RefMessage(false, myNodeId), global, address, topic);
    }

    private static void submitSend(final Object message, final boolean global, final long address, final Object topic) throws SuspendExecution {
        LOG.debug("sending: " + message);
        sendThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    staticSend(message, global, address, topic);
                    LOG.debug("sent {}", message);
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }
        });
    }

    private static void staticSend(Object message, final boolean global, final long address, final Object topic) throws SuspendExecution {
        try {
            if (global) {
                final long ref = address;
                if (message instanceof Streamable) {
                    if (topic instanceof String)
                        getMessenger().sendToOwnerOf(ref, (String) topic, (Streamable) message);
                    else
                        getMessenger().sendToOwnerOf(ref, (Long) topic, (Streamable) message);
                } else {
                    final byte[] buf = Serialization.getInstance().write(message);
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
                    final byte[] buf = Serialization.getInstance().write(message);
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

    private short getNodeId() {
        return getCluster().getMyNodeId();
    }

    static class CloseMessage implements Serializable {
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
        final public boolean globalCopy;
        final public long addressCopy;
        final public Object topicCopy;

        public RCPhantomReference(GlxRemoteChannel referent) {
            super(referent, q);
            this.topicCopy = referent.topic;
            this.addressCopy = referent.address;
            this.globalCopy = referent.global;
            this.myNodeId = referent.getNodeId();
        }

        public void unregister() throws SuspendExecution {
            GlxRemoteChannel.unregisterRemoteRef(myNodeId, globalCopy, addressCopy, topicCopy);
            rcs.remove(this);
        }

        public void register() throws SuspendExecution {
            rcs.add(this);
            GlxRemoteChannel.registerRemoteRef(myNodeId, globalCopy, addressCopy, topicCopy);
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
