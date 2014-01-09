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

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.common.util.ServiceUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import jsr166e.ConcurrentHashMapV8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry used to find registered actors by name. Actors are registered with the {@link Actor#register() } method.
 *
 * @author pron
 */
public class ActorRegistry {
    // TODO: there are probably race conditions here
    private static final Logger LOG = LoggerFactory.getLogger(ActorRegistry.class);
    private static final ConcurrentMap<String, Entry> registeredActors = new ConcurrentHashMapV8<String, Entry>();
    private static final GlobalRegistry globalRegistry = ServiceUtil.loadSingletonServiceOrNull(GlobalRegistry.class);

    static {
        LOG.info("Global registry is {}", globalRegistry);
    }

    static Object register(Actor<?, ?> actor) {
        final String name = actor.getName();
        if (name == null)
            throw new IllegalArgumentException("name is null");

        // atomically register
        final ActorRef ref = actor.ref();
        final Entry old = registeredActors.get(name);
        if (old != null && old.actor == actor.ref())
            return old.globalId;

        if (old != null && LocalActor.isLocal(old.actor) && !LocalActor.isDone(old.actor))
            throw new RegistrationException("Actor " + old + " is not dead and is already registered under " + name);

        if (old != null)
            LOG.info("Re-registering {}: old was {}", name, old);

        if (old != null && !registeredActors.remove(name, old))
            throw new RegistrationException("Concurrent registration under the name " + name);

        final Entry entry = new Entry(null, ref);
        if (registeredActors.putIfAbsent(name, entry) != null)
            throw new RegistrationException("Concurrent registration under the name " + name);

        LOG.info("Registering {}: {}", name, actor);

        final Object globalId = globalRegistry != null ? registerGlobal(actor.ref()) : name;
        entry.globalId = globalId;

        actor.monitor();

        return globalId;
    }

    static private Object registerGlobal(final ActorRef<?> actor) {
        try {
            return new Fiber<Object>() {
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
    }

    static void unregister(final String name) {
        LOG.info("Unregistering actor: {}", name);

        if (globalRegistry != null) {
            // TODO: will only work if called from a fiber
            try {
                new Fiber<Void>() {
                    @Override
                    protected Void run() throws SuspendExecution, InterruptedException {
                        globalRegistry.unregister(registeredActors.get(name).actor);
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

    /**
     * Locates a registered actor by name.
     *
     * @param name the actor's name.
     * @return the actor, or {@code null} if no actor by that name is currently registered.
     */
    public static <Message> ActorRef<Message> getActor(final String name) {
        Entry entry = registeredActors.get(name);
        ActorRef<Message> actor = entry != null ? (ActorRef<Message>) entry.actor : null;

        if (actor == null && globalRegistry != null) {
            // TODO: will only work if called from a fiber
            try {
                actor = new Fiber<ActorRef<Message>>() {
                    @Override
                    protected ActorRef<Message> run() throws SuspendExecution, InterruptedException {
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

    /**
     * Checks whether the registry is global to the entire cluster.
     *
     * @return {@code true} if the registry is global to the entire cluster, or {@code false} if it is local to this JVM instance.
     */
    public static boolean hasGlobalRegistry() {
        return globalRegistry != null;
    }

    private static class Entry {
        Object globalId;
        final ActorRef<?> actor;

        public Entry(Object globalId, ActorRef<?> actor) {
            this.globalId = globalId;
            this.actor = actor;
        }
    }
}
