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
package co.paralleluniverse.actors;

import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.ServiceUtil;
import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry used to find registered actors by name. Actors are registered with the {@link Actor#register() } method.
 *
 * @author pron
 */
public class ActorRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ActorRegistry.class);
    private static final co.paralleluniverse.actors.spi.ActorRegistry registry;

    static {
        co.paralleluniverse.actors.spi.ActorRegistry tmp = ServiceUtil.loadSingletonServiceOrNull(co.paralleluniverse.actors.spi.ActorRegistry.class);
        if (tmp == null)
            tmp = new LocalActorRegistry();
        registry = tmp;
        LOG.info("Actor registry is {}", registry);

    }

    static <Message> void register(Actor<Message, ?> actor) throws SuspendExecution {
        final String name = actor.getName();
        if (name == null)
            throw new IllegalArgumentException("name is null");
        LOG.info("Registering {}: {}", name, actor);

        actor.preRegister(name);
        registry.register(actor, actor.ref0());
        actor.postRegister();

        actor.monitor();
    }

    static <Message> void unregister(Actor<Message, ?> actor) {
        LOG.info("Unregistering actor: {}", actor.getName());

        registry.unregister(actor, actor.ref());
    }

    /**
     * Locates a registered actor by name.
     *
     * @param name the actor's name.
     * @return the actor, or {@code null} if no actor by that name is currently registered.
     */
    public static <T extends ActorRef<?>> T tryGetActor(String name) throws SuspendExecution {
        return (T) registry.tryGetActor(name);
    }

    /**
     * Locates a registered actor by name, or blocks until one is registered, but no more than the given timeout.
     *
     * @param name    the actor's name.
     * @param timeout the timeout
     * @param unit    the timeout's unit
     * @return the actor, or {@code null} if the timeout expires before one is registered.
     */
    public static <T extends ActorRef<?>> T getActor(String name, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        return (T) registry.getActor(name, timeout, unit);
    }

    /**
     * Locates a registered actor by name, or blocks until one is registered.
     *
     * @param name the actor's name.
     * @return the actor.
     */
    public static <T extends ActorRef<?>> T getActor(String name) throws InterruptedException, SuspendExecution {
        return getActor(name, 0, null);
    }

    /**
     * Locates a registered actor by name, or, if not actor by that name is currently registered, spawns and registers it.
     * This method atomically checks if an actor by the given name is registers, and if so, returns it; otherwise it spawns and registers the
     * actor returned by the given factory.
     *
     * @param name         the actor's name.
     * @param actorFactory returns an actor that will be registered if one isn't currently registered.
     * @param scheduler    the {@link FiberScheduler} to use when spawning the actor, or {@code null} to spawn the fiber using the default scheduler.
     * @return the actor.
     */
    public static <Message> ActorRef<Message> getOrRegisterActor(final String name, final Callable<Actor<Message, ?>> actorFactory, final FiberScheduler scheduler) throws SuspendExecution {
        Callable<ActorRef<Message>> factory = new Callable<ActorRef<Message>>() {

            @Override
            public ActorRef<Message> call() throws Exception {
                Actor actor = actorFactory.call();
                actor.preRegister(name);
                final FiberFactory ff = scheduler;
                return scheduler != null ? actor.spawn(ff) : actor.spawnThread();
            }
        };
        ActorRef<Message> actor = registry.getOrRegisterActor(name, factory);
        LocalActor.postRegister(actor);
        return actor;
    }

    /**
     * Locates a registered actor by name, or, if not actor by that name is currently registered, spawns and registers it.
     * This method atomically checks if an actor by the given name is registers, and if so, returns it; otherwise it spawns the actor
     * returned by the given factory using the default fiber scheduler, and registers it.
     *
     * @param name         the actor's name.
     * @param actorFactory returns an actor that will be registered if one isn't currently registered.
     * @return the actor.
     */
    public static <Message> ActorRef<Message> getOrRegisterActor(String name, Callable<Actor<Message, ?>> actorFactory) throws SuspendExecution {
        return getOrRegisterActor(name, actorFactory, DefaultFiberScheduler.getInstance());
    }

    /**
     * Checks whether the registry is global to the entire cluster.
     *
     * @return {@code true} if the registry is global to the entire cluster, or {@code false} if it is local to this JVM instance.
     */
    public static boolean hasGlobalRegistry() {
        return !(registry instanceof LocalActorRegistry);
    }

    /**
     * Shuts down the registry.
     */
    public static void shutdown() {
        registry.shutdown();
    }

    /**
     * Clears the registry (use only in tests!).
     */
    public static void clear() {
        if (!Debug.isUnitTest())
            throw new IllegalStateException("Must only be called in unit tests");
        if (registry instanceof LocalActorRegistry)
            ((LocalActorRegistry) registry).clear();
        else
            throw new UnsupportedOperationException();
    }
}
