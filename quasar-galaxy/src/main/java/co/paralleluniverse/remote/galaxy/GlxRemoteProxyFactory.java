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
package co.paralleluniverse.remote.galaxy;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.io.serialization.KryoSerializer;
import co.paralleluniverse.remote.RemoteProxyFactory;
import co.paralleluniverse.strands.channels.SendPort;

/**
 *
 * @author pron
 */
public class GlxRemoteProxyFactory implements RemoteProxyFactory {

    @Override
    public <Message> GlxRemoteActor<Message> create(ActorRef<Message> actor, Object globalId) {
        return new GlxRemoteActor<Message>(actor, globalId);
    }

    @Override
    public <Message> SendPort<Message> create(SendPort<Message> channel, Object globalId) {
        return new GlxRemoteChannel<Message>(channel, globalId);
    }
}
