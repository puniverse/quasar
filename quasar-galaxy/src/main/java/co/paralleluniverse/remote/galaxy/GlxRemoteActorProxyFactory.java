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
import co.paralleluniverse.actors.spi.RemoteActorProxyFactory;
import org.kohsuke.MetaInfServices;

/**
 *
 * @author pron
 */
@MetaInfServices
public class GlxRemoteActorProxyFactory implements RemoteActorProxyFactory {

    @Override
    public <Message> GlxRemoteActor<Message> create(ActorRef<Message> actor, Object globalId) {
        return globalId != null ? new GlxGlobalRemoteActor<Message>(actor, globalId) : new GlxNonGlobalRemoteActor<Message>(actor);
    }
}
