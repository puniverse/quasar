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

import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.io.serialization.KryoSerializer;
import co.paralleluniverse.remote.RemoteProxyFactory;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.SendChannel;

/**
 *
 * @author pron
 */
public class GalaxyRemoteProxyFactory implements RemoteProxyFactory {
    static {
        KryoSerializer.register(RemoteChannel.class);
        KryoSerializer.register(RemoteActor.class);
        KryoSerializer.register(RemoteActor.getActorLifecycleListenerClass());
        KryoSerializer.register(RemoteChannel.CloseMessage.class);
        KryoSerializer.register(co.paralleluniverse.actors.ExitMessage.class);
        KryoSerializer.register(co.paralleluniverse.actors.ShutdownMessage.class);
    }

    @Override
    public <Message> RemoteActor<Message> create(LocalActor<Message, ?> actor, Object globalId) {
        return new RemoteActor<Message>(actor, globalId);
    }

    @Override
    public <Message> SendChannel<Message> create(Channel channel, Object globalId) {
        return new RemoteChannel<Message>(channel, globalId);
    }
}
