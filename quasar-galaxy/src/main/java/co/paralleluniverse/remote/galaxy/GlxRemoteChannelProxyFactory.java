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

import co.paralleluniverse.remote.RemoteChannelProxyFactory;
import co.paralleluniverse.strands.channels.SendPort;
import org.kohsuke.MetaInfServices;

/**
 *
 * @author pron
 */
@MetaInfServices
public class GlxRemoteChannelProxyFactory implements RemoteChannelProxyFactory {
    @Override
    public <Message> SendPort<Message> create(SendPort<Message> channel, Object globalId) {
        return new GlxRemoteChannel<Message>(channel, globalId);
    }
}
