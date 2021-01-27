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
package co.paralleluniverse.actors;

import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.concurrent.ReentrantLock;
import com.google.common.base.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry used to find registered actors by name. Actors are registered with the {@link Actor#register() } method.
 *
 * @author pron
 */
class LocalActorRegistry extends co.paralleluniverse.actors.spi.ActorRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(LocalActorRegistry.class);
    private final ConcurrentMap<String, ActorRef<?>> registeredActors = MapUtil.newConcurrentHashMap();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition cond = lock.newCondition();

    @Override
    @Suspendable
    public <Message> void register(Actor<Message, ?> actor, ActorRef<Message> actorRef) {
        final String name = actorRef.getName();
        if (name == null)
            throw new IllegalArgumentException("name is null");

        lock.lock();
        try {
            final ActorRef<?> old = registeredActors.get(name);
            if (old != null && Objects.equal(old, actorRef))
                return;

            if (old != null && LocalActor.isLocal(old) && !LocalActor.isDone(old))
                throw new RegistrationException("Actor " + old + " is not dead and is already registered under " + name);

            if (old != null)
                LOG.info("Re-registering {}: old was {}", name, old);

            if (old != null && !registeredActors.remove(name, old))
                throw new RegistrationException("Concurrent registration under the name " + name);

            if (registeredActors.putIfAbsent(name, actorRef) != null)
                throw new RegistrationException("Concurrent registration under the name " + name);

            cond.signalAll();
        } finally {
            lock.unlock();
        }
        LOG.info("Registering {}: {}", name, actorRef);
    }

    @Override
    public <Message> void unregister(Actor<Message, ?> actor, ActorRef<Message> actorRef) {
        registeredActors.remove(actorRef.getName());
    }

    @Override
    public ActorRef<?> tryGetActor(final String name) throws SuspendExecution {
        ActorRef<?> actor = registeredActors.get(name);
        if (actor == null) {
            lock.lock();
            try {
                actor = registeredActors.get(name);
            } finally {
                lock.unlock();
            }
        }
        return actor;
    }

    @Override
    public ActorRef<?> getActor(final String name) throws InterruptedException, SuspendExecution {
        return getActor(name, 0, null);
    }

    @Override
    public ActorRef<?> getActor(final String name, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        ActorRef<?> actor = registeredActors.get(name);
        if (actor == null) {
            final long deadline = unit != null ? System.nanoTime() + unit.toNanos(timeout) : 0;
            lock.lock();
            try {
                for (;;) {
                    actor = registeredActors.get(name);
                    if (actor != null)
                        break;

                    if (deadline > 0) {
                        final long now = System.nanoTime();
                        if (now > deadline)
                            return null;
                        cond.await(deadline - now, TimeUnit.NANOSECONDS);
                    } else
                        cond.await();
                }

            } finally {
                lock.unlock();
            }
        }
        return actor;
    }

    @Override
    public <T extends ActorRef<?>> T getOrRegisterActor(final String name, Callable<T> actorFactory) throws SuspendExecution {
        T actor = (T)registeredActors.get(name);
        if (actor == null) {
            lock.lock();
            try {
                actor = (T)registeredActors.get(name);
                if (actor == null) {
                    try {
                        actor = actorFactory.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    LocalActor.register(actor, name);
                }
            } finally {
                lock.unlock();
            }
        }
        return actor;
    }

    /**
     * Use only in tests!
     */
    void clear() {
        registeredActors.clear();
    }

    @Override
    public void shutdown() {
    }
}
