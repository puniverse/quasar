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

import java.util.concurrent.ConcurrentMap;
import jsr166e.ConcurrentHashMapV8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class ActorRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ActorRegistry.class);
    private static final ConcurrentMap<Object, LocalActor> registeredActors = new ConcurrentHashMapV8<Object, LocalActor>();

    public static void register(LocalActor actor) {
        final Object name = actor.getName();
        if (name == null)
            throw new IllegalArgumentException("name is null");

        // atomically register
        final Actor old = registeredActors.get(name);
        if (old != null && !old.isDone())
            throw new RegistrationException("Actor " + old + " is not dead and is already registered under " + name);
        
        if(old != null)
            LOG.info("Re-registering {}: old was {}", name, old);
        
        if (old != null && !registeredActors.remove(name, old))
            throw new RegistrationException("Concurrent registration under the name " + name);
        if (registeredActors.putIfAbsent(name, actor) != null)
            throw new RegistrationException("Concurrent registration under the name " + name);

        if(old != null)
            LOG.info("Registering {}: {}", name, actor);
        
        actor.monitor();
    }

    public static void unregister(Object name) {
        LOG.info("Unregistering {}: {}", name);
        registeredActors.remove(name);
    }

    public static Actor getActor(Object name) {
        return registeredActors.get(name);
    }
}
