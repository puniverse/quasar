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

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.spi.ActorRegistry;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.AbstractCacheListener;
import co.paralleluniverse.galaxy.Cache;
import co.paralleluniverse.galaxy.CacheListener;
import co.paralleluniverse.galaxy.StoreTransaction;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.galaxy.quasar.Grid;
import co.paralleluniverse.galaxy.quasar.Store;
import co.paralleluniverse.io.serialization.Serialization;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
@MetaInfServices
public class GlxGlobalRegistry extends ActorRegistry {
    static volatile GlxGlobalRegistry INSTANCE;

    private static final ConcurrentHashMap<String, CacheEntry> rootCache = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(GlxGlobalRegistry.class);
    private static final ReentrantLock lock = new ReentrantLock();
    private final Grid grid;

    @SuppressWarnings("LeakingThisInConstructor")
    public GlxGlobalRegistry() {
        assert INSTANCE == null;
        try {
            grid = Grid.getInstance();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        INSTANCE = this;
    }

    @Override
    public <Message> void register(Actor<Message, ?> actor, ActorRef<Message> actorRef) throws SuspendExecution {
        final String rootName = actorRef.getName();

        LOG.info("Registering actor {} at root {}", actorRef, rootName);

        final Store store = grid.store();
        StoreTransaction txn = store.beginTransaction();
        lock.lock();
        try {
            try {
                Object globalId = getGlobalId(actor);
                final long root = store.getRoot(rootName, globalId != null ? (Long) globalId : -1, txn);
                // assert globalId == null || ((Long) globalId) == root; -- it's OK to replace the actor's globalId -- until it's too late
                setGlobalId(actor, root);
                store.getx(root, txn);
                store.set(root, Serialization.getInstance().write(actorRef), txn);
                final long version = store.getVersion(root);
                LOG.debug("Registered actor {} at rootId  {}", actorRef, Long.toHexString(root));
                store.commit(txn);
                
                updateCache(rootName, new CacheEntry(actorRef, root, version)); // <--- comment out to test "distribution" on a single node
            } catch (TimeoutException e) {
                LOG.error("Registering actor {} at root {} failed due to timeout", actorRef, rootName);
                store.rollback(txn);
                store.abort(txn);
                throw new RuntimeException("Actor registration failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <Message> void unregister(Actor<Message, ?> actor, final ActorRef<Message> actorRef) {
        new Fiber<Void>() {

            @Override
            protected Void run() throws SuspendExecution, InterruptedException {
                unregister0(actorRef);
                return null;
            }
        };
    }

    private void unregister0(ActorRef<?> actor) throws SuspendExecution {
        final String rootName = actor.getName();

        LOG.info("Unregistering {}", rootName);

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
    public ActorRef<?> tryGetActor(String name) throws SuspendExecution {
        ActorRef<?> actor = tryCache(name);
        if (actor != null)
            return actor;
        return updateCache(name, tryGetActor0(name));
    }

    @Override
    public ActorRef<?> getActor(String name) throws InterruptedException, SuspendExecution {
//        try {
        return getActor(name, 0, null);
//        } catch (java.util.concurrent.TimeoutException e) {
//            throw new AssertionError(e);
//        }
    }

    @Override
    public ActorRef<?> getActor(String name, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        ActorRef<?> actor = tryCache(name);
        if (actor != null)
            return actor;
        return updateCache(name, getActor0(name, timeout, unit));
    }

    @Override
    public <T extends ActorRef<?>> T getOrRegisterActor(String name, Callable<T> actorFactory) throws SuspendExecution {
        ActorRef<?> actor = tryCache(name);
        if (actor != null)
            return (T) actor;
        return updateCache(name, getOrRegisterActor0(name, actorFactory));
    }

    private ActorRef<?> tryCache(String name) {
        CacheEntry entry = rootCache.get(name);
        if (entry == null)
            return null;
        return entry.version == grid.store().getVersion(entry.root) ? entry.actor : null;
    }

    private CacheEntry tryGetActor0(final String rootName) throws SuspendExecution, RuntimeException {
        final Store store = grid.store();
        final StoreTransaction txn = store.beginTransaction();
        try {
            try {
                final long root = store.getRoot(rootName, txn);
                final byte[] buf = store.gets(root, txn);
                if (buf == null) {
                    LOG.debug("Store returned null for root {}", rootName);
                    return null;
                }
                final long version = store.getVersion(root);
                store.commit(txn);

                LOG.debug("Store returned a buffer ({} bytes) for root {}", buf.length, rootName);

                if (buf.length == 0)
                    return null; // TODO: Galaxy should return null

                ActorRef<?> actor = deserActor(rootName, buf);
                return new CacheEntry(actor, root, version);
            } catch (TimeoutException e) {
                LOG.error("Getting actor {} failed due to timeout", rootName);
                store.rollback(txn);
                store.abort(txn);
                throw new RuntimeException("Actor discovery failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private CacheEntry getActor0(final String rootName, long timeout, TimeUnit unit) throws SuspendExecution, RuntimeException, InterruptedException {
        final long deadline = unit != null ? System.nanoTime() + unit.toNanos(timeout) : 0;
        final Store store = grid.store();

        final long root;
        final ReentrantLock lck0 = new ReentrantLock();
        final Condition cond = lck0.newCondition();
        boolean listening = false;

        final StoreTransaction txn = store.beginTransaction();
        try {
            root = store.getRoot(rootName, txn);

            final CacheListener listener = new AbstractCacheListener() {
                @Override
                public void evicted(Cache cache, long id) {
                    invalidated(cache, id);
                }

                @Override
                public void invalidated(Cache cache, long id) {
                    grid.getDelegate().store().getAsync(id);
                }

                @Override
                public void received(Cache cache, long id, long version, ByteBuffer data) {
                    if (data != null && data.remaining() > 0) {
                        LOG.debug("Received root {} ({})", rootName, Long.toHexString(id));
                        lck0.lock();
                        try {
                            cond.signalAll();
                        } finally {
                            lck0.unlock();
                        }
                        store.setListener(root, null);
                    }
                }
            };
            listening = (store.setListenerIfAbsent(root, listener) == listener);

            store.commit(txn);
        } catch (TimeoutException e) {
            LOG.error("Getting actor {} failed due to timeout", rootName);
            store.rollback(txn);
            store.abort(txn);
            throw new RuntimeException("Actor discovery failed");
        }

        try {
            byte[] buf = store.gets(root, null);
            long version = store.getVersion(root);
            store.release(root);

            if (listening) {
                lck0.lock();
                try {
                    while (buf == null || buf.length == 0) {
                        LOG.debug("Store returned null for root {}", rootName);

                        if (deadline > 0) {
                            final long now = System.nanoTime();
                            if (now > deadline)
                                return null; // throw new java.util.concurrent.TimeoutException();
                            cond.await(deadline - now, TimeUnit.NANOSECONDS);
                        } else
                            cond.await();

                        buf = store.gets(root, null);
                        version = store.getVersion(root);
                        store.release(root);
                    }
                } finally {
                    lck0.unlock();
                }
            } else
                assert buf != null && buf.length > 0;

            final ActorRef<?> actor = deserActor(rootName, buf);
            return new CacheEntry(actor, root, version);
        } catch (TimeoutException e) {
            LOG.error("Getting actor {} failed due to timeout", rootName);
            throw new RuntimeException("Actor discovery failed");
        }
    }

    private CacheEntry getOrRegisterActor0(final String rootName, Callable<? extends ActorRef<?>> actorFactory) throws SuspendExecution, RuntimeException {
        final Store store = grid.store();
        final StoreTransaction txn = store.beginTransaction();
        try {
            try {
                final long root = store.getRoot(rootName, txn);

                final ActorRef<?> actor;
                final byte[] buf = store.getx(root, txn);
                long version = store.getVersion(root);

                if (buf == null || buf.length == 0) {
                    try {
                        actor = actorFactory.call();
                    } catch (Exception e) {
                        throw new RuntimeException("Exception while creating actor", e);
                    }
                    LOG.debug("Store returned null for root {}. Registering actor {} at rootId  {}", rootName, actor, root);

                    store.set(root, Serialization.getInstance().write(actor), txn);
                    version = store.getVersion(root);
                } else
                    actor = deserActor(rootName, buf);

                store.commit(txn);

                return new CacheEntry(actor, root, version);
            } catch (TimeoutException e) {
                LOG.error("Getting actor {} failed due to timeout", rootName);
                store.rollback(txn);
                store.abort(txn);
                throw new RuntimeException("Actor discovery/registration failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private <Message> ActorRef<Message> deserActor(final String rootName, byte[] buf) {
        try {
            final ActorRef<Message> actor = (ActorRef<Message>) Serialization.getInstance().read(buf);
            LOG.debug("Deserialized actor {} for root {}", actor, rootName);
            return actor;
        } catch (Exception e) {
            LOG.error("Deserializing actor at root " + rootName + " has failed with exception", e);
            return null;
        }
    }

    private <T extends ActorRef<?>> T updateCache(final String rootName, CacheEntry entry) {
        rootCache.put(rootName, entry);
        return (T) entry.actor;
    }

    @Override
    public void shutdown() {
        grid.cluster().goOffline();
    }

    static class CacheEntry {
        final ActorRef<?> actor;
        final long root;
        final long version;

        public CacheEntry(ActorRef<?> actor, long root, long version) {
            this.actor = actor;
            this.root = root;
            this.version = version;
        }
    }
}
