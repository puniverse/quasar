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
package co.paralleluniverse.remote;

import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.RemoteActor;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.SendChannel;
import java.util.ArrayList;
import java.util.ServiceLoader;

/**
 *
 * @author pron
 */
public class RemoteProxyFactoryService {
    private static final RemoteProxyFactory factory;
    
    static {
        final ServiceLoader<RemoteProxyFactory> loader = ServiceLoader.load(RemoteProxyFactory.class);
        
        ArrayList<RemoteProxyFactory> factories = new ArrayList<>();
        for(RemoteProxyFactory f : loader)
            factories.add(f);
        
        if(factories.size() == 1)
            factory = factories.iterator().next();
        else {
            if(factories.isEmpty())
                throw new Error("No implementation of " + RemoteProxyFactory.class.getName() + " found!");
            else
                throw new Error("Several implementations of " + RemoteProxyFactory.class.getName() + " found: " + factories);
        }
    }
    
    public static <Message> RemoteActor<Message> create(LocalActor<Message, ?> actor) {
        return factory.create(actor);
    }
    
    public <Message> SendChannel<Message> create(Channel channel) {
        return factory.create(channel);
    }
}
