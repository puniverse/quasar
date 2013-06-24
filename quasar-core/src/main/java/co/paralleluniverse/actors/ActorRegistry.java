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

import co.paralleluniverse.remote.GlobalRegistry;
import co.paralleluniverse.remote.ServiceUtil;
import java.util.concurrent.ConcurrentMap;
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

    static Object register(LocalActor<?, ?> actor) {
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
        if (globalRegistry != null)
            globalId = globalRegistry.register(actor);
        else
            globalId = name;

        actor.monitor();

        return globalId;
    }

    static void unregister(Object name) {
        LOG.info("Unregistering actor: {}", name);
        
        if (globalRegistry != null)
            globalRegistry.unregister(name);
        
        registeredActors.remove(name);
    }

    public static <Message> Actor<Message> getActor(Object name) {
        Actor<Message> actor = registeredActors.get(name);
        
        if(actor == null && globalRegistry != null)
            actor = globalRegistry.getActor(name);
        
        return actor;
    }
}
