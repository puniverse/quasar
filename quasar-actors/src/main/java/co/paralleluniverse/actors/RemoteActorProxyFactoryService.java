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

import co.paralleluniverse.common.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public final class RemoteActorProxyFactoryService {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteActorProxyFactoryService.class);
    private static final RemoteActorProxyFactory factory = ServiceUtil.loadSingletonService(RemoteActorProxyFactory.class);

    static {
        LOG.info("RemoteActorProxyFactory is {}", factory);
    }
    
    public static <Message> RemoteActorRef<Message> create(ActorRef<Message> actor, Object globalId) {
        return factory.create(actor, globalId);
    }

    private RemoteActorProxyFactoryService() {
    }
}
