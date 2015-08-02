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
import co.paralleluniverse.actors.spi.ActorRegistry;
import co.paralleluniverse.fibers.Fiber;
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
public class GlxGlobalRegistry implements ActorRegistry {
    static volatile GlxGlobalRegistry INSTANCE;

    private static final ConcurrentHashMap<String, ActorRef> rootCache = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(GlxGlobalRegistry.class);
    private static final ReentrantLock lock = new ReentrantLock();
    private final co.paralleluniverse.galaxy.Grid grid1;
    private final Grid grid;

    @SuppressWarnings("LeakingThisInConstructor")
    public GlxGlobalRegistry() {
        assert INSTANCE == null;
        try {
            grid1 = co.paralleluniverse.galaxy.Grid.getInstance();
            grid = new Grid(grid1);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        INSTANCE = this;
    }

    @Override
    public Object register(ActorRef<?> actor, Object globalId) throws SuspendExecution {
        final String rootName = actor.getName();

        LOG.info("Registering actor {} at root {}", actor, rootName);

        final Store store = grid.store();
        StoreTransaction txn = store.beginTransaction();
        lock.lock();
        try {
            try {
                final long root = store.getRoot(rootName, globalId != null ? (Long) globalId : -1, txn);
                // assert globalId == null || ((Long) globalId) == root; -- it's OK to replace the actor's globalId -- until it's too late
                store.getx(root, txn);
                store.set(root, Serialization.getInstance().write(actor), txn);
                LOG.debug("Registered actor {} at rootId  {}", actor, Long.toHexString(root));
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
            lock.unlock();
        }
    }

    @Override
    public void unregister(final ActorRef<?> actor) {
        new Fiber<Void>() {

            @Override
            protected Void run() throws SuspendExecution, InterruptedException {
                unregister0(actor);
                return null;
            }
        };
    }

    private void unregister0(ActorRef<?> actor) throws SuspendExecution {
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
    public <Message> ActorRef<Message> tryGetActor(String name) throws SuspendExecution {
        ActorRef cacheValue = rootCache.get(name);
        if (cacheValue != null)
            return cacheValue;
        cacheValue = rootCache.get(name);
        if (cacheValue != null)
            return cacheValue;
        return tryGetActor0(name);
    }

    @Override
    public <Message> ActorRef<Message> getActor(String name) throws InterruptedException, SuspendExecution {
//        try {
        return getActor(name, 0, null);
//        } catch (java.util.concurrent.TimeoutException e) {
//            throw new AssertionError(e);
//        }
    }

    @Override
    public <Message> ActorRef<Message> getActor(String name, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        ActorRef cacheValue = rootCache.get(name);
        if (cacheValue != null)
            return cacheValue;
        cacheValue = rootCache.get(name);
        if (cacheValue != null)
            return cacheValue;
        return getActor0(name, 0, null);
    }

    @Override
    public <Message> ActorRef<Message> getOrRegisterActor(String name, Callable<ActorRef<Message>> actorFactory) throws SuspendExecution {
        ActorRef cacheValue = rootCache.get(name);
        if (cacheValue != null)
            return cacheValue;
        cacheValue = rootCache.get(name);
        if (cacheValue != null)
            return cacheValue;
        return getOrRegisterActor0(name, actorFactory);
    }

    private <Message> ActorRef<Message> tryGetActor0(final String rootName) throws SuspendExecution, RuntimeException {
        final Store store = grid.store();
        final StoreTransaction txn = store.beginTransaction();
        try {
            try {
                final long root = store.getRoot(rootName, txn);
                byte[] buf = store.get(root);
                if (buf == null) {
                    LOG.debug("Store returned null for root {}", rootName);
                    return null;
                }
                store.commit(txn);

                LOG.debug("Store returned a buffer ({} bytes) for root {}", buf.length, rootName);

                if (buf.length == 0)
                    return null; // TODO: Galaxy should return null

                return (ActorRef<Message>) updateCache(rootName, root, deserActor(rootName, buf));
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

    private <Message> ActorRef<Message> getActor0(final String rootName, long timeout, TimeUnit unit) throws SuspendExecution, RuntimeException, InterruptedException {
        final long deadline = unit != null ? System.nanoTime() + unit.toNanos(timeout) : 0;
        final Store store = grid.store();

        final long root;
        final ReentrantLock lck0 = new ReentrantLock();
        final Condition cond = lck0.newCondition();

        final StoreTransaction txn = store.beginTransaction();
        try {
            root = store.getRoot(rootName, txn);

            store.setListener(root, new AbstractCacheListener() {
                @Override
                public void evicted(Cache cache, long id) {
                    invalidated(cache, id);
                }

                @Override
                public void invalidated(Cache cache, long id) {
                    grid1.store().getAsync(id);
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
            });

            store.commit(txn);
        } catch (TimeoutException e) {
            LOG.error("Getting actor {} failed due to timeout", rootName);
            store.rollback(txn);
            store.abort(txn);
            throw new RuntimeException("Actor discovery failed");
        }

        try {
            byte[] buf = store.get(root);

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
                    buf = store.get(root);
                }
            } finally {
                lck0.unlock();
            }

            return (ActorRef<Message>) updateCache(rootName, root, deserActor(rootName, buf));
        } catch (TimeoutException e) {
            LOG.error("Getting actor {} failed due to timeout", rootName);
            throw new RuntimeException("Actor discovery failed");
        }
    }

    private <Message> ActorRef<Message> getOrRegisterActor0(final String rootName, Callable<ActorRef<Message>> actorFactory) throws SuspendExecution, RuntimeException {
        final Store store = grid.store();
        final StoreTransaction txn = store.beginTransaction();
        try {
            try {
                final long root = store.getRoot(rootName, txn);

                final ActorRef<Message> actor;
                byte[] buf = store.getx(root, txn);
                if (buf == null || buf.length == 0) {
                    try {
                        actor = actorFactory.call();
                    } catch (Exception e) {
                        throw new RuntimeException("Exception while creating actor", e);
                    }
                    LOG.debug("Store returned null for root {}. Registering actor {} at rootId  {}", rootName, actor, root);

                    store.set(root, Serialization.getInstance().write(actor), txn);
                } else
                    actor = deserActor(rootName, buf);

                store.commit(txn);

                return (ActorRef<Message>) updateCache(rootName, root, actor);
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

    private ActorRef<?> updateCache(final String rootName, long root, ActorRef<?> actor) {
        final Store store = grid.store();
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
    }

    void evict(String name, ActorRef<?> actor) {
        rootCache.remove(name, actor);
    }

    @Override
    public void shutdown() {
        grid.cluster().goOffline();
    }
}
