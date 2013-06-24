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
package co.paralleluniverse.actors;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.DefaultFiberPool;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.remote.GlobalRegistry;
import co.paralleluniverse.remote.ServiceUtil;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import jsr166e.ConcurrentHashMapV8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class ActorRegistry {
    // TODO: there are probably race conditions here
    private static final Logger LOG = LoggerFactory.getLogger(ActorRegistry.class);
    private static final ConcurrentMap<Object, LocalActor> registeredActors = new ConcurrentHashMapV8<Object, LocalActor>();
    private static final GlobalRegistry globalRegistry = ServiceUtil.loadSingletonServiceOrNull(GlobalRegistry.class);

    static Object register(final LocalActor<?, ?> actor) {
        final Object name = actor.getName();
        if (name == null)
            throw new IllegalArgumentException("name is null");

        // atomically register
        final LocalActor old = registeredActors.get(name);
        if (old == actor)
            return old.getGlobalId();

        if (old != null && !old.isDone())
            throw new RegistrationException("Actor " + old + " is not dead and is already registered under " + name);

        if (old != null)
            LOG.info("Re-registering {}: old was {}", name, old);

        if (old != null && !registeredActors.remove(name, old))
            throw new RegistrationException("Concurrent registration under the name " + name);
        if (registeredActors.putIfAbsent(name, actor) != null)
            throw new RegistrationException("Concurrent registration under the name " + name);

        LOG.info("Registering {}: {}", name, actor);

        final Object globalId;
        if (globalRegistry != null) {
            try {
                globalId = new Fiber<Object>(DefaultFiberPool.getInstance()) {
                    @Override
                    protected Object run() throws SuspendExecution, InterruptedException {
                        return globalRegistry.register(actor);
                    }
                }.start().get();
            } catch (ExecutionException e) {
                throw Exceptions.rethrow(e.getCause());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else
            globalId = name;

        actor.monitor();

        return globalId;
    }

    static void unregister(final Object name) {
        LOG.info("Unregistering actor: {}", name);

        if (globalRegistry != null) {
            // TODO: will only work if called from a fiber
            try {
                new Fiber<Void>(DefaultFiberPool.getInstance()) {
                    @Override
                    protected Void run() throws SuspendExecution, InterruptedException {
                        globalRegistry.unregister(name);
                        return null;
                    }
                }.start().join();
            } catch (ExecutionException e) {
                throw Exceptions.rethrow(e.getCause());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        registeredActors.remove(name);
    }

    public static <Message> Actor<Message> getActor(final Object name) {
        Actor<Message> actor = registeredActors.get(name);

        if (actor == null && globalRegistry != null) {
            // TODO: will only work if called from a fiber
            try {
                actor = new Fiber<Actor<Message>>(DefaultFiberPool.getInstance()) {
                    @Override
                    protected Actor<Message> run() throws SuspendExecution, InterruptedException {
                        return globalRegistry.getActor(name);
                    }
                }.start().get();
            } catch (ExecutionException e) {
                throw Exceptions.rethrow(e.getCause());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return actor;
    }
}
