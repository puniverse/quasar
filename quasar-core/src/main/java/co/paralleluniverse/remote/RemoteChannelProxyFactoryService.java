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
package co.paralleluniverse.remote;

import co.paralleluniverse.common.util.ServiceUtil;
import co.paralleluniverse.strands.channels.SendPort;

/**
 *
 * @author pron
 */
public final class RemoteChannelProxyFactoryService {
    private static final RemoteChannelProxyFactory factory = ServiceUtil.loadSingletonService(RemoteChannelProxyFactory.class);
    
    public static <Message> SendPort<Message> create(SendPort<Message> channel, Object globalId) {
        return factory.create(channel, globalId);
    }

    private RemoteChannelProxyFactoryService() {
    }
}
