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
import co.paralleluniverse.strands.SuspendableCallable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry used to find registered actors by name. Actors are registered with the {@link Actor#register() } method.
 *
 * @author pron
 */
public class ActorRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ActorRegistry.class);
    private static final co.paralleluniverse.actors.spi.ActorRegistry globalRegistry = ServiceUtil.loadSingletonServiceOrNull(co.paralleluniverse.actors.spi.ActorRegistry.class);
    private static final LocalActorRegistry localRegistry = new LocalActorRegistry();

    static {
        LOG.info("Global registry is {}", globalRegistry);
    }

    static Object register(final Actor<?, ?> actor, final Object globalId) {
        final String name = actor.getName();
        if (name == null)
            throw new IllegalArgumentException("name is null");
        LOG.info("Registering {}: {}", name, actor);

        final Object res;
        if (globalRegistry != null)
            res = runInFiber(new SuspendableCallable<Object>() {

                @Override
                public Object run() throws SuspendExecution, InterruptedException {
                    return globalRegistry.register(actor.ref(), globalId);
                }
            });
        else
            res = localRegistry.register(actor.ref(), globalId);

        actor.monitor();

        return res;
    }

    static void unregister(final ActorRef<?> actor) {
        LOG.info("Unregistering actor: {}", actor.getName());

        if (globalRegistry != null) {
            runInFiber(new SuspendableCallable<Void>() {
                @Override
                public Void run() throws SuspendExecution, InterruptedException {
                    globalRegistry.unregister(actor);
                    return null;
                }
            });
        } else
            localRegistry.unregister(actor);
    }

    /**
     * Locates a registered actor by name.
     *
     * @param name the actor's name.
     * @return the actor, or {@code null} if no actor by that name is currently registered.
     */
    public static <Message> ActorRef<Message> tryGetActor(final String name) throws SuspendExecution {
        final ActorRef<Message> actor;
        if (globalRegistry != null)
            actor = runInFiber(new SuspendableCallable<ActorRef<Message>>() {
                @Override
                public ActorRef<Message> run() throws SuspendExecution, InterruptedException {
                    return globalRegistry.getActor(name);
                }
            });
        else
            actor = localRegistry.tryGetActor(name);

        return actor;
    }

    /**
     * Locates a registered actor by name, or blocks until one is registered, but no more than the given timeout.
     *
     * @param name    the actor's name.
     * @param timeout the timeout
     * @param unit    the timeout's unit
     * @return the actor, or {@code null} if the timeout expires before one is registered.
     */
    public static <Message> ActorRef<Message> getActor(final String name, final long timeout, final TimeUnit unit) throws InterruptedException, SuspendExecution {
        final ActorRef<Message> actor;
        if (globalRegistry != null)
            actor = runInFiber(new SuspendableCallable<ActorRef<Message>>() {
                @Override
                public ActorRef<Message> run() throws SuspendExecution, InterruptedException {
                    return globalRegistry.getActor(name, timeout, unit);
                }
            });
        else
            actor = localRegistry.getActor(name, timeout, unit);

        return actor;
    }

    /**
     * Locates a registered actor by name, or blocks until one is registered.
     *
     * @param name the actor's name.
     * @return the actor.
     */
    public static <Message> ActorRef<Message> getActor(final String name) throws InterruptedException, SuspendExecution {
        return getActor(name, 0, null);
    }

    /**
     * Locates a registered actor by name, or, if not actor by that name is currently registered, creates and registers it.
     * This method atomically checks if an actor by the given name is registers, and if so, returns it; otherwise it registers the
     * actor returned by the given factory.
     *
     * @param name         the actor's name.
     * @param actorFactory returns an actor that will be registered if one isn't currently registered.
     * @return the actor.
     */
    public static <Message> ActorRef<Message> getOrRegisterActor(final String name, final Callable<ActorRef<Message>> actorFactory) throws SuspendExecution {
        final ActorRef<Message> actor;
        if (globalRegistry != null)
            actor = runInFiber(new SuspendableCallable<ActorRef<Message>>() {
                @Override
                public ActorRef<Message> run() throws SuspendExecution, InterruptedException {
                    return globalRegistry.getOrRegisterActor(name, actorFactory);
                }
            });
        else
            actor = localRegistry.getOrRegisterActor(name, actorFactory);

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

    public static void shutdown() {
        if (globalRegistry != null)
            globalRegistry.shutdown();
    }

    private static <T> T runInFiber(SuspendableCallable<T> target) {
        try {
            return new Fiber<T>(target).start().joinNoSuspend().get();
        } catch (ExecutionException e) {
            throw Exceptions.rethrow(e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
