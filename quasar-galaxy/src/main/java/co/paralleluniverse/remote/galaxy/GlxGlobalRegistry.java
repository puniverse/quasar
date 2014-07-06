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

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.spi.GlobalRegistry;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.AbstractCacheListener;
import co.paralleluniverse.galaxy.Cache;
import co.paralleluniverse.galaxy.StoreTransaction;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.galaxy.quasar.Grid;
import co.paralleluniverse.galaxy.quasar.Store;
import co.paralleluniverse.io.serialization.Serialization;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
@MetaInfServices
public class GlxGlobalRegistry implements GlobalRegistry {
    private static final ConcurrentHashMap<String, ActorRef> rootCache = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(GlxGlobalRegistry.class);
    private static final Serialization ser = Serialization.getInstance();
    private static final ReentrantLock serlock = new ReentrantLock();
    private final Grid grid;

    public GlxGlobalRegistry() {
        try {
            grid = new Grid(co.paralleluniverse.galaxy.Grid.getInstance());
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object register(ActorRef<?> actor, Object globalId) throws SuspendExecution {
        final String rootName = actor.getName();

        LOG.info("Registering actor {} at root {}", actor, rootName);

        final Store store = grid.store();
        StoreTransaction txn = store.beginTransaction();
        serlock.lock();
        try {
            try {
                final long root = store.getRoot(rootName, globalId != null ? (Long) globalId : -1, txn);
                // assert globalId == null || ((Long) globalId) == root;
                if (globalId != null && ((Long) globalId) != root)
                    throw new AssertionError();
                store.getx(root, txn);
                store.set(root, ser.write(actor), txn);
                LOG.debug("commit Registering actor {} at rootId  {}", actor, root);
                store.commit(txn);
                return root; // root is the global id
            } catch (TimeoutException e) {
                LOG.error("Registering actor {} at root {} failed due to timeout", actor, rootName);
                store.rollback(txn);
                store.abort(txn);
                throw new RuntimeException("Actor registration failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            serlock.unlock();
        }
    }

    @Override
    public void unregister(ActorRef<?> actor) throws SuspendExecution {
        final String rootName = actor.getName();

        LOG.info("Uregistering {}", rootName);

        final Store store = grid.store();

        StoreTransaction txn = store.beginTransaction();
        try {
            try {
                final long root = store.getRoot(rootName, txn);
                store.set(root, (byte[]) null, txn);
                store.commit(txn);
            } catch (TimeoutException e) {
                LOG.error("Unregistering {} failed due to timeout", rootName);
                store.rollback(txn);
                store.abort(txn);
                throw new RuntimeException("Actor unregistration failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <Message> ActorRef<Message> getActor(String name) throws SuspendExecution {
        final String rootName = name;
        ActorRef cacheValue = rootCache.get(rootName);
        if (cacheValue != null)
            return cacheValue;
        serlock.lock();
        try {
            cacheValue = rootCache.get(rootName);
            if (cacheValue != null)
                return cacheValue;
            return getRootFromStoreAndUpdateCache(rootName);
        } finally {
            serlock.unlock();
        }
    }

    private <Message> ActorRef<Message> getRootFromStoreAndUpdateCache(final String rootName) throws SuspendExecution, RuntimeException {
        final Store store = grid.store();

        StoreTransaction txn = store.beginTransaction();
        try {
            boolean error = false;
            try {
                
                final long root = store.getRoot(rootName, txn);
//                Condition cond = serlock.newCondition();
//                
//                store.setListener(root, new AbstractCacheListener() {
//                    @Override
//                    public void received(Cache cache, long id, long version, ByteBuffer data) {
//                        cond.signalAll();
//                    }
//                });
                
                byte[] buf = store.get(root);
                if (buf == null) {
                    LOG.debug("Store returned null for root {}", rootName);
                    return null;
                }

                LOG.debug("Store returned a buffer ({} bytes) for root {}", buf.length, rootName);
                
                if (buf.length == 0)
                    return null; // TODO: Galaxy should return null

                final ActorRef<Message> actor;
                try {
                    actor = (ActorRef<Message>) ser.read(buf);
                } catch (Exception e) {
                    LOG.error("Deserializing actor at root " + rootName + " has failed with exception", e);
                    return null;
                }
                
                LOG.debug("Deserialized actor {} for root {}", actor, rootName);
                
                store.setListener(root, new AbstractCacheListener() {
                    @Override
                    public void invalidated(Cache cache, long id) {
                        evicted(cache, id);
                    }

                    @Override
                    public void received(Cache cache, long id, long version, ByteBuffer data) {
                        evicted(cache, id);
                    }

                    @Override
                    public void evicted(Cache cache, long id) {
                        rootCache.remove(rootName);
                        store.setListener(id, null);
                    }
                });
                rootCache.put(rootName, actor);
                return actor;
            } catch (TimeoutException e) {
                error = true;
                LOG.error("Getting actor {} failed due to timeout", rootName);
                store.rollback(txn);
                store.abort(txn);
                throw new RuntimeException("Actor discovery failed");
            } finally {
                if (!error)
                    store.commit(txn);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        grid.cluster().goOffline();
    }
}
