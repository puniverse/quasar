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
package co.paralleluniverse.actors.galaxy;

import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.remote.RemoteProxyFactory;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.SendChannel;
import co.paralleluniverse.strands.channels.galaxy.RemoteChannel;

/**
 *
 * @author pron
 */
public class GalaxyRemoteProxyFactory implements RemoteProxyFactory {
    @Override
    public <Message> RemoteActor<Message> create(LocalActor<Message, ?> actor, Object globalId) {
        return new RemoteActor<Message>(actor, globalId);
    }

    @Override
    public <Message> SendChannel<Message> create(Channel channel, Object globalId) {
        return new RemoteChannel<Message>(channel, globalId);
    }
}
